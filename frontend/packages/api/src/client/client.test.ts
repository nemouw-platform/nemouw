import { describe, expect, it, vi } from "vitest"

import { createAuthApi } from "../endpoints"
import { createApiClient } from "."
import { ApiError } from "../errors"

const BASE = "http://api.test"

function json(status: number, body: unknown, headers: HeadersInit = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", ...headers },
  })
}

function error(status: number, code: string, extra: object = {}) {
  return json(status, { status, code, message: code, ...extra })
}

/** Fake fetch answering each "METHOD /path" from a queue of responses */
function fakeFetch(
  routes: Record<string, Array<() => Response | Promise<Response>>>
) {
  const calls: Array<{ key: string; init: RequestInit }> = []
  const fetch = vi.fn(
    async (input: RequestInfo | URL, init: RequestInit = {}) => {
      const key = `${init.method} ${String(input).replace(BASE, "")}`
      calls.push({ key, init })
      const next = routes[key]?.shift()
      if (!next) throw new Error(`Unexpected request: ${key}`)
      return next()
    }
  )
  return { fetch: fetch as unknown as typeof globalThis.fetch, calls }
}

describe("createApiClient", () => {
  it("sends JSON with credentials to the /v1 prefix and parses the response", async () => {
    const { fetch, calls } = fakeFetch({
      "POST /v1/auth/signup": [() => json(201, { email: "a@b.co" })],
    })
    const auth = createAuthApi(createApiClient({ baseUrl: `${BASE}/`, fetch }))

    const result = await auth.signup({
      role: "specialist",
      first_name: "Omar",
      last_name: "Antar",
      email: "a@b.co",
      locale: "ar",
    })

    expect(result).toEqual({ email: "a@b.co" })
    const { init } = calls[0]!
    expect(init.credentials).toBe("include")
    expect(new Headers(init.headers).get("Content-Type")).toBe(
      "application/json"
    )
    expect(JSON.parse(init.body as string)).toMatchObject({
      first_name: "Omar",
      locale: "ar",
    })
  })

  it("returns undefined for empty 202/204 responses", async () => {
    const { fetch } = fakeFetch({
      "POST /v1/auth/resend-otp": [() => new Response(null, { status: 202 })],
      "POST /v1/auth/logout": [() => new Response(null, { status: 204 })],
    })
    const auth = createAuthApi(createApiClient({ baseUrl: BASE, fetch }))

    await expect(auth.resendOtp({ email: "a@b.co" })).resolves.toBeUndefined()
    await expect(auth.logout()).resolves.toBeUndefined()
  })

  it("lets fetch set the multipart boundary for FormData", async () => {
    const { fetch, calls } = fakeFetch({
      "POST /v1/upload": [() => json(200, {})],
    })
    await createApiClient({ baseUrl: BASE, fetch }).post(
      "/upload",
      new FormData()
    )

    expect(new Headers(calls[0]!.init.headers).has("Content-Type")).toBe(false)
  })

  it("turns the standard error body into an ApiError with field errors", async () => {
    const { fetch } = fakeFetch({
      "POST /v1/auth/signup": [
        () =>
          error(422, "VALIDATION_ERROR", {
            trace_id: "trace-1",
            errors: [{ field: "email", code: "MUST_BE_EMAIL", message: "bad" }],
          }),
      ],
    })
    const client = createApiClient({ baseUrl: BASE, fetch })

    const err = await client.post("/auth/signup", {}).catch((e: unknown) => e)

    expect(err).toBeInstanceOf(ApiError)
    expect(err).toMatchObject({
      status: 422,
      code: "VALIDATION_ERROR",
      traceId: "trace-1",
    })
    expect((err as ApiError).fieldError("email")?.code).toBe("MUST_BE_EMAIL")
    expect((err as ApiError).fieldError("first_name")).toBeUndefined()
  })

  it("reads Retry-After on 429", async () => {
    const { fetch } = fakeFetch({
      "POST /v1/auth/resend-otp": [
        () =>
          json(
            429,
            { status: 429, code: "TOO_MANY_REQUESTS", message: "slow down" },
            {
              "Retry-After": "42",
            }
          ),
      ],
    })

    const err = await createApiClient({ baseUrl: BASE, fetch })
      .post("/auth/resend-otp", {})
      .catch((e: unknown) => e)

    expect(err).toMatchObject({ code: "TOO_MANY_REQUESTS", retryAfter: 42 })
  })

  it("maps responses without the error body to UNEXPECTED_RESPONSE", async () => {
    const { fetch } = fakeFetch({
      "GET /v1/countries": [
        () => new Response("<html>Bad gateway</html>", { status: 502 }),
      ],
    })

    const err = await createApiClient({ baseUrl: BASE, fetch })
      .get("/countries")
      .catch((e: unknown) => e)

    expect(err).toMatchObject({ status: 502, code: "UNEXPECTED_RESPONSE" })
  })

  it("maps network failures to NETWORK_ERROR with status 0", async () => {
    const fetch = vi.fn(async () => {
      throw new TypeError("Failed to fetch")
    }) as unknown as typeof globalThis.fetch

    const err = await createApiClient({ baseUrl: BASE, fetch })
      .get("/countries")
      .catch((e: unknown) => e)

    expect(err).toMatchObject({ status: 0, code: "NETWORK_ERROR" })
  })

  it("rethrows aborts untouched", async () => {
    const controller = new AbortController()
    controller.abort()
    const fetch = vi.fn(async () => {
      throw new DOMException("Aborted", "AbortError")
    }) as unknown as typeof globalThis.fetch

    const err = await createApiClient({ baseUrl: BASE, fetch })
      .get("/countries", { signal: controller.signal })
      .catch((e: unknown) => e)

    expect(err).toBeInstanceOf(DOMException)
  })
})

describe("session refresh", () => {
  it.each(["INVALID_TOKEN", "UNAUTHENTICATED"])(
    "refreshes once and retries after 401 %s",
    async (code) => {
      const { fetch, calls } = fakeFetch({
        "GET /v1/specialists/me": [
          () => error(401, code),
          () => json(200, { id: 1 }),
        ],
        "POST /v1/auth/refresh": [() => json(200, { role: "specialist" })],
      })

      const result = await createApiClient({ baseUrl: BASE, fetch }).get(
        "/specialists/me"
      )

      expect(result).toEqual({ id: 1 })
      expect(calls.map((c) => c.key)).toEqual([
        "GET /v1/specialists/me",
        "POST /v1/auth/refresh",
        "GET /v1/specialists/me",
      ])
    }
  )

  it("shares one refresh between concurrent 401s", async () => {
    let releaseRefresh!: () => void
    const refreshGate = new Promise<void>(
      (resolve) => (releaseRefresh = resolve)
    )
    const { fetch, calls } = fakeFetch({
      "GET /v1/a": [() => error(401, "INVALID_TOKEN"), () => json(200, "a")],
      "GET /v1/b": [() => error(401, "INVALID_TOKEN"), () => json(200, "b")],
      "POST /v1/auth/refresh": [async () => (await refreshGate, json(200, {}))],
    })
    const client = createApiClient({ baseUrl: BASE, fetch })

    const both = Promise.all([client.get("/a"), client.get("/b")])
    await vi.waitFor(() =>
      expect(
        calls.filter((c) => c.key === "POST /v1/auth/refresh")
      ).toHaveLength(1)
    )
    releaseRefresh()

    await expect(both).resolves.toEqual(["a", "b"])
    expect(calls.filter((c) => c.key === "POST /v1/auth/refresh")).toHaveLength(
      1
    )
  })

  it("reports an expired session when the refresh fails", async () => {
    const onSessionExpired = vi.fn()
    const { fetch } = fakeFetch({
      "GET /v1/specialists/me": [() => error(401, "INVALID_TOKEN")],
      "POST /v1/auth/refresh": [() => error(401, "INVALID_TOKEN")],
    })

    const err = await createApiClient({
      baseUrl: BASE,
      fetch,
      onSessionExpired,
    })
      .get("/specialists/me")
      .catch((e: unknown) => e)

    expect(err).toMatchObject({ status: 401, code: "INVALID_TOKEN" })
    expect(onSessionExpired).toHaveBeenCalledTimes(1)
  })

  it("never refreshes for auth endpoints", async () => {
    const { fetch, calls } = fakeFetch({
      "POST /v1/auth/login": [() => error(401, "INVALID_CREDENTIALS")],
    })

    const err = await createAuthApi(createApiClient({ baseUrl: BASE, fetch }))
      .login({ identifier: "a@b.co", password: "wrong-password" })
      .catch((e: unknown) => e)

    expect(err).toMatchObject({ code: "INVALID_CREDENTIALS" })
    expect(calls).toHaveLength(1)
  })

  it("does not refresh when autoRefresh is off (server-side clients)", async () => {
    const { fetch, calls } = fakeFetch({
      "GET /v1/specialists/me": [() => error(401, "INVALID_TOKEN")],
    })

    await createApiClient({ baseUrl: BASE, fetch, autoRefresh: false })
      .get("/specialists/me")
      .catch(() => undefined)

    expect(calls).toHaveLength(1)
  })

  it("forwards extra headers, e.g. cookies on the server", async () => {
    const { fetch, calls } = fakeFetch({
      "GET /v1/specialists/me": [() => json(200, {})],
    })

    await createApiClient({
      baseUrl: BASE,
      fetch,
      headers: () => ({ Cookie: "access_token=abc" }),
    }).get("/specialists/me")

    expect(new Headers(calls[0]!.init.headers).get("Cookie")).toBe(
      "access_token=abc"
    )
  })
})
