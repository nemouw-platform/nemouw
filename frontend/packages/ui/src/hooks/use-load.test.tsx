// @vitest-environment jsdom
import { act, renderHook } from "@testing-library/react"
import { describe, expect, it, vi } from "vitest"

import { useLoad } from "@workspace/ui/hooks/use-load"

const flush = () => act(async () => {})

describe("useLoad", () => {
  it("loads once on mount and exposes the data", async () => {
    const load = vi.fn().mockResolvedValue(["a", "b"])
    const { result, rerender } = renderHook(() => useLoad(load))

    expect(result.current.isLoading).toBe(true)
    await flush()
    rerender()

    expect(load).toHaveBeenCalledOnce()
    expect(load.mock.calls[0]![0]).toBeInstanceOf(AbortSignal)
    expect(result.current).toMatchObject({
      data: ["a", "b"],
      isLoading: false,
      failed: false,
    })
  })

  it("reports a failure and loads again on retry", async () => {
    const onSuccess = vi.fn()
    const load = vi
      .fn()
      .mockRejectedValueOnce(new Error("offline"))
      .mockResolvedValue("ok")
    const { result } = renderHook(() => useLoad(load, { onSuccess }))

    await flush()
    expect(result.current.failed).toBe(true)
    expect(result.current.isLoading).toBe(false)

    act(() => result.current.retry())
    expect(result.current.isLoading).toBe(true)
    await flush()

    expect(result.current).toMatchObject({ data: "ok", failed: false })
    expect(onSuccess).toHaveBeenCalledExactlyOnceWith("ok")
  })

  it("waits out a failure retryDelay accepts, staying in loading", async () => {
    vi.useFakeTimers()
    const onError = vi.fn()
    const throttled = new Error("429")
    const load = vi
      .fn()
      .mockRejectedValueOnce(throttled)
      .mockResolvedValue("ok")
    const { result } = renderHook(() =>
      useLoad(load, {
        retryDelay: (error) => (error === throttled ? 1000 : null),
        onError,
      })
    )

    await flush()
    expect(result.current).toMatchObject({ isLoading: true, failed: false })
    expect(load).toHaveBeenCalledOnce()

    await act(() => vi.advanceTimersByTimeAsync(1000))
    expect(load).toHaveBeenCalledTimes(2)
    expect(result.current).toMatchObject({ data: "ok", failed: false })
    expect(onError).not.toHaveBeenCalled()
    vi.useRealTimers()
  })

  it("gives up after a few automatic retries", async () => {
    vi.useFakeTimers()
    const onError = vi.fn()
    const load = vi.fn().mockRejectedValue(new Error("429"))
    const { result } = renderHook(() =>
      useLoad(load, { retryDelay: () => 1000, onError })
    )

    await flush()
    for (let i = 0; i < 3; i++) {
      await act(() => vi.advanceTimersByTimeAsync(1000))
    }

    expect(load).toHaveBeenCalledTimes(4)
    expect(result.current.failed).toBe(true)
    expect(onError).toHaveBeenCalledOnce()
    vi.useRealTimers()
  })

  it("fails at once when retryDelay says null", async () => {
    const load = vi.fn().mockRejectedValue(new Error("404"))
    const { result } = renderHook(() =>
      useLoad(load, { retryDelay: () => null })
    )
    await flush()
    expect(result.current.failed).toBe(true)
    expect(load).toHaveBeenCalledOnce()
  })

  it("aborts the load on unmount", async () => {
    let signal: AbortSignal | undefined
    const { unmount } = renderHook(() =>
      useLoad((s) => {
        signal = s
        return new Promise(() => undefined)
      })
    )
    await flush()

    unmount()
    expect(signal?.aborted).toBe(true)
  })
})
