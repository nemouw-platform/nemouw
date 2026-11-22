// @vitest-environment jsdom
import { act, renderHook } from "@testing-library/react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import {
  isAbortError,
  useRequest,
  type RequestFn,
} from "@workspace/ui/hooks/use-request"

/** A promise the test resolves or rejects by hand */
function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason: unknown) => void
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

/** Request fn whose calls the test settles one by one */
function controllable<T>() {
  const calls: Array<
    { args: unknown[]; signal: AbortSignal } & ReturnType<typeof deferred<T>>
  > = []
  const fn = vi.fn((signal: AbortSignal, ...args: unknown[]) => {
    const d = deferred<T>()
    calls.push({ args, signal, ...d })
    return d.promise
  })
  return { fn: fn as unknown as RequestFn<[string], T>, calls }
}

const flush = () => act(async () => {})

beforeEach(() => vi.useFakeTimers())
afterEach(() => vi.useRealTimers())

describe("useRequest", () => {
  it("debounces 300 ms by default and sends only the last call", async () => {
    const { fn, calls } = controllable<string>()
    const { result } = renderHook(() => useRequest(fn))

    act(() => {
      result.current.run("n")
      result.current.run("ne")
      result.current.run("nem")
    })
    expect(result.current.isPending).toBe(true)

    act(() => void vi.advanceTimersByTime(299))
    expect(calls).toHaveLength(0)

    act(() => void vi.advanceTimersByTime(1))
    expect(calls).toHaveLength(1)
    expect(calls[0]!.args).toEqual(["nem"])

    calls[0]!.resolve("result")
    await flush()
    expect(result.current).toMatchObject({
      status: "success",
      data: "result",
      isPending: false,
    })
  })

  it("sends immediately with debounceMs: 0", () => {
    const { fn, calls } = controllable<string>()
    const { result } = renderHook(() => useRequest(fn, { debounceMs: 0 }))

    act(() => result.current.run("now"))

    expect(calls).toHaveLength(1)
  })

  it("aborts the request in flight when a newer call starts, and ignores its late response", async () => {
    const { fn, calls } = controllable<string>()
    const { result } = renderHook(() => useRequest(fn, { debounceMs: 0 }))

    act(() => result.current.run("first"))
    act(() => result.current.run("second"))

    expect(calls[0]!.signal.aborted).toBe(true)
    expect(calls[1]!.signal.aborted).toBe(false)

    calls[1]!.resolve("second result")
    await flush()
    calls[0]!.resolve("first result (late)")
    await flush()

    expect(result.current.data).toBe("second result")
  })

  it("aborts the request in flight as soon as a new call is debounced", () => {
    const { fn, calls } = controllable<string>()
    const { result } = renderHook(() => useRequest(fn))

    act(() => result.current.run("a"))
    act(() => void vi.advanceTimersByTime(300))
    act(() => result.current.run("ab"))

    expect(calls[0]!.signal.aborted).toBe(true)
  })

  it("resolves every runAsync call in the debounce window with the winning result", async () => {
    const { fn, calls } = controllable<string>()
    const { result } = renderHook(() => useRequest(fn))

    let first!: Promise<string>
    let second!: Promise<string>
    act(() => {
      first = result.current.runAsync("a")
      second = result.current.runAsync("b")
    })
    act(() => void vi.advanceTimersByTime(300))
    calls[0]!.resolve("for b")

    await expect(first).resolves.toBe("for b")
    await expect(second).resolves.toBe("for b")
  })

  it("rejects runAsync with the error, stores it, and calls onError once", async () => {
    const { fn, calls } = controllable<string>()
    const onError = vi.fn()
    const { result } = renderHook(() =>
      useRequest(fn, { debounceMs: 0, onError })
    )

    let pending!: Promise<string>
    act(() => {
      pending = result.current.runAsync("x")
    })
    const failure = new Error("boom")
    calls[0]!.reject(failure)

    await expect(pending).rejects.toBe(failure)
    await flush()
    expect(result.current).toMatchObject({ status: "error", error: failure })
    expect(onError).toHaveBeenCalledExactlyOnceWith(failure)
  })

  it("run() keeps errors in state instead of throwing", async () => {
    vi.useRealTimers() // Node reports unhandled rejections on a real macrotask
    const { fn, calls } = controllable<string>()
    const unhandled = vi.fn()
    process.on("unhandledRejection", unhandled)
    const { result } = renderHook(() => useRequest(fn, { debounceMs: 0 }))

    act(() => result.current.run("x"))
    calls[0]!.reject(new Error("boom"))
    await flush()
    await new Promise((r) => setImmediate(r))

    process.off("unhandledRejection", unhandled)
    expect(result.current.status).toBe("error")
    expect(unhandled).not.toHaveBeenCalled()
  })

  it("keeps data while pending and clears the error on the next success", async () => {
    const { fn, calls } = controllable<string>()
    const { result } = renderHook(() => useRequest(fn, { debounceMs: 0 }))

    act(() => result.current.run("1"))
    calls[0]!.resolve("one")
    await flush()
    act(() => result.current.run("2"))
    expect(result.current).toMatchObject({ status: "pending", data: "one" })

    calls[1]!.reject(new Error("fail"))
    await flush()
    expect(result.current).toMatchObject({ status: "error", data: "one" })

    act(() => result.current.run("3"))
    calls[2]!.resolve("three")
    await flush()
    expect(result.current).toMatchObject({
      status: "success",
      data: "three",
      error: undefined,
    })
  })

  it("cancel() aborts, rejects waiters with an AbortError and restores the status", async () => {
    const { fn, calls } = controllable<string>()
    const { result } = renderHook(() => useRequest(fn, { debounceMs: 0 }))

    act(() => result.current.run("1"))
    calls[0]!.resolve("one")
    await flush()

    let pending!: Promise<string>
    act(() => {
      pending = result.current.runAsync("2")
    })
    act(() => result.current.cancel())

    expect(calls[1]!.signal.aborted).toBe(true)
    const error = await pending.catch((e: unknown) => e)
    expect(isAbortError(error)).toBe(true)
    expect(result.current).toMatchObject({ status: "success", data: "one" })
  })

  it("cancel() during the debounce never sends the request", () => {
    const { fn, calls } = controllable<string>()
    const { result } = renderHook(() => useRequest(fn))

    act(() => result.current.run("x"))
    act(() => result.current.cancel())
    act(() => void vi.advanceTimersByTime(1000))

    expect(calls).toHaveLength(0)
    expect(result.current.status).toBe("idle")
  })

  it("reset() clears data and error", async () => {
    const { fn, calls } = controllable<string>()
    const { result } = renderHook(() => useRequest(fn, { debounceMs: 0 }))

    act(() => result.current.run("1"))
    calls[0]!.resolve("one")
    await flush()
    act(() => result.current.reset())

    expect(result.current).toMatchObject({
      status: "idle",
      data: undefined,
      error: undefined,
    })
  })

  it("aborts on unmount and rejects pending runAsync calls", async () => {
    const { fn, calls } = controllable<string>()
    const onSuccess = vi.fn()
    const { result, unmount } = renderHook(() =>
      useRequest(fn, { debounceMs: 0, onSuccess })
    )

    let pending!: Promise<string>
    act(() => {
      pending = result.current.runAsync("x")
    })
    unmount()

    expect(calls[0]!.signal.aborted).toBe(true)
    calls[0]!.resolve("too late")
    const error = await pending.catch((e: unknown) => e)
    expect(isAbortError(error)).toBe(true)
    expect(onSuccess).not.toHaveBeenCalled()
  })

  it("uses the latest request function without resetting the debounce", async () => {
    const first = vi.fn(async () => "first")
    const second = vi.fn(async () => "second")
    const { result, rerender } = renderHook(({ fn }) => useRequest(fn), {
      initialProps: { fn: first as RequestFn<[], string> },
    })

    act(() => result.current.run())
    rerender({ fn: second })
    act(() => void vi.advanceTimersByTime(300))
    await flush()

    expect(first).not.toHaveBeenCalled()
    expect(second).toHaveBeenCalledOnce()
    expect(result.current.data).toBe("second")
  })

  it("restart() retries the last call after an error", async () => {
    const { fn, calls } = controllable<string>()
    const { result } = renderHook(() => useRequest(fn))

    act(() => result.current.run("query"))
    act(() => void vi.advanceTimersByTime(300))
    calls[0]!.reject(new Error("offline"))
    await flush()
    expect(result.current.status).toBe("error")

    act(() => result.current.restart())
    expect(result.current.isPending).toBe(true)
    expect(calls[1]!.args).toEqual(["query"]) // sent without waiting for the debounce

    calls[1]!.resolve("back online")
    await flush()
    expect(result.current).toMatchObject({
      status: "success",
      data: "back online",
      error: undefined,
    })
  })

  it("restart() does nothing before the first call or after reset()", async () => {
    const { fn, calls } = controllable<string>()
    const { result } = renderHook(() => useRequest(fn, { debounceMs: 0 }))

    act(() => result.current.restart())
    expect(calls).toHaveLength(0)
    expect(result.current.status).toBe("idle")

    act(() => result.current.run("query"))
    calls[0]!.resolve("data")
    await flush()
    act(() => result.current.reset())
    act(() => result.current.restart())

    expect(calls).toHaveLength(1)
    expect(result.current.status).toBe("idle")
  })
})
