import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

const assign = vi.fn()

let tooManyRequests: () => Error

/**
 * A fresh module per test: endSession remembers that it already ran. The
 * 429 comes from the same fresh graph, or isApiError would not recognize it
 */
async function load(pathname: string, search = "") {
  vi.stubGlobal("window", { location: { pathname, search, assign } })
  vi.resetModules()
  const { ApiError } = await import("@workspace/api")
  tooManyRequests = () =>
    new ApiError(
      { status: 429, code: "TOO_MANY_REQUESTS", message: "Slow down" },
      1
    )
  return (await import("@/features/auth/session/end-session")).endSession
}

describe("endSession", () => {
  beforeEach(() => {
    vi.useFakeTimers()
    assign.mockClear()
  })
  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it("clears the cookies, then opens login with next back here", async () => {
    const endSession = await load("/ar/onboarding", "?tab=1")
    const clear = vi.fn().mockResolvedValue(undefined)

    endSession(clear)
    await vi.runAllTimersAsync()

    expect(clear).toHaveBeenCalledOnce()
    expect(assign).toHaveBeenCalledWith(
      "/ar/login?next=%2Fonboarding%3Ftab%3D1"
    )
  })

  it("runs once when several calls fail together", async () => {
    const endSession = await load("/en/onboarding")
    const clear = vi.fn().mockResolvedValue(undefined)

    endSession(clear)
    endSession(clear)
    await vi.runAllTimersAsync()

    expect(clear).toHaveBeenCalledOnce()
    expect(assign).toHaveBeenCalledOnce()
  })

  it("tries the logout again after a 429's Retry-After", async () => {
    const endSession = await load("/en/onboarding")
    const clear = vi
      .fn()
      .mockRejectedValueOnce(tooManyRequests())
      .mockResolvedValue(undefined)

    endSession(clear)
    await vi.advanceTimersByTimeAsync(999)
    expect(clear).toHaveBeenCalledOnce()
    expect(assign).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(1)
    expect(clear).toHaveBeenCalledTimes(2)
    expect(assign).toHaveBeenCalledOnce()
  })

  it("stops after three rate-limited tries, and still opens login", async () => {
    const endSession = await load("/en/onboarding")
    const clear = vi.fn().mockRejectedValue(tooManyRequests())

    endSession(clear)
    await vi.runAllTimersAsync()

    expect(clear).toHaveBeenCalledTimes(3)
    expect(assign).toHaveBeenCalledOnce()
  })

  it("does not retry other failures", async () => {
    const endSession = await load("/en/onboarding")
    const clear = vi.fn().mockRejectedValue(new Error("offline"))

    endSession(clear)
    await vi.runAllTimersAsync()

    expect(clear).toHaveBeenCalledOnce()
    expect(assign).toHaveBeenCalledOnce()
  })

  it("stays on a guest page such as login", async () => {
    const endSession = await load("/en/login")

    endSession(vi.fn().mockResolvedValue(undefined))
    await vi.runAllTimersAsync()

    expect(assign).not.toHaveBeenCalled()
  })
})
