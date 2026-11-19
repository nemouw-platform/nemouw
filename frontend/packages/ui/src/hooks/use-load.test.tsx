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

  it("keeps the data on screen while it loads again", async () => {
    const load = vi
      .fn()
      .mockResolvedValueOnce(["a"])
      .mockReturnValueOnce(new Promise(() => undefined))
    const { result } = renderHook(() => useLoad(load))
    await flush()

    act(() => result.current.retry())
    expect(result.current).toMatchObject({
      data: ["a"],
      isLoading: false,
      failed: false,
    })
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
