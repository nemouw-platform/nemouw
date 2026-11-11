import type { ApiClientOptions, HttpMethod, RequestOptions } from "./types"
import { ApiError, CLIENT_ERROR_CODES } from "../errors"

/** Every backend route lives under this prefix */
export const API_PREFIX = "/v1"

/** Sends one HTTP request to the backend and returns the raw response */
export interface HttpTransport {
  send(
    method: HttpMethod,
    path: string,
    options?: RequestOptions
  ): Promise<Response>
}

type TransportOptions = Pick<ApiClientOptions, "baseUrl" | "headers" | "fetch">

/**
 * Builds the URL, headers and body, always sends cookies (the session lives in
 * HttpOnly cookies), and maps network failures to ApiError NETWORK_ERROR.
 */
export function createHttpTransport(options: TransportOptions): HttpTransport {
  const baseUrl = options.baseUrl.replace(/\/+$/, "") + API_PREFIX
  const doFetch: typeof fetch =
    options.fetch ?? ((input, init) => globalThis.fetch(input, init))

  return {
    async send(method, path, init = {}) {
      const headers = await buildHeaders(options.headers, init.headers)
      const body = encodeBody(init.body, headers)

      try {
        return await doFetch(baseUrl + path, {
          method,
          headers,
          body,
          credentials: "include",
          signal: init.signal,
        })
      } catch (error) {
        if (init.signal?.aborted) throw error // let callers see their own abort
        throw networkError(error)
      }
    },
  }
}

async function buildHeaders(
  defaults: TransportOptions["headers"],
  perRequest: HeadersInit | undefined
): Promise<Headers> {
  const headers = new Headers(await defaults?.())
  new Headers(perRequest).forEach((value, key) => headers.set(key, value))
  headers.set("Accept", "application/json")
  return headers
}

/** JSON-encodes the body; FormData passes through so fetch sets the multipart boundary */
function encodeBody(body: unknown, headers: Headers): BodyInit | undefined {
  if (body === undefined) return undefined
  if (body instanceof FormData) return body
  headers.set("Content-Type", "application/json")
  return JSON.stringify(body)
}

function networkError(cause: unknown): ApiError {
  return new ApiError({
    status: 0,
    code: CLIENT_ERROR_CODES.network,
    message: cause instanceof Error ? cause.message : "Network request failed",
  })
}
