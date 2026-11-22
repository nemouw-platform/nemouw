import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import {
  createRequestRunner,
  isAbortError,
} from "@workspace/ui/hooks/use-request/request-runner"
import type { RequestFn } from "@workspace/ui/hooks/use-request/types"

/** Request whose calls the test settles one by one */
function controllableRequest() {
  const calls: Array<{
    args: unknown[]
    signal: AbortSignal
    resolve: (value: string) => void
    reject: (reason: unknown) => void
  }> = []
  const request: RequestFn<[string], string> = (signal, ...args) =>
    new Promise((resolve, reject) =>
      calls.push({ args, signal, resolve, reject })
    )
  return { request, calls }
}

function setup(delayMs = 300) {
  const { request, calls } = controllableRequest()
  const events = {
    started: vi.fn(),
    succeeded: vi.fn(),
    failed: vi.fn(),
    cancelled: vi.fn(),
  }
  const runner = createRequestRunner({
    getRequest: () => request,
    getDelayMs: () => delayMs,
    events,
  })
  return { runner, events, calls }
}

beforeEach(() => vi.useFakeTimers())
afterEach(() => vi.useRealTimers())

describe("createRequestRunner", () => {
  it("waits for the delay, then sends only the last call", () => {
    const { runner, calls } = setup()

    runner.run("a")
    runner.run("ab")
    vi.advanceTimersByTime(299)
    expect(calls).toHaveLength(0)

    vi.advanceTimersByTime(1)
    expect(calls).toHaveLength(1)
    expect(calls[0]!.args).toEqual(["ab"])
  })

  it("sends at once with a delay of 0", () => {
    const { runner, calls } = setup(0)

    runner.run("now")

    expect(calls).toHaveLength(1)
  })

  it("resolves every caller of a debounce window with the winning result", async () => {
    const { runner, calls, events } = setup()

    const first = runner.run("a")
    const second = runner.run("ab")
    vi.advanceTimersByTime(300)
    calls[0]!.resolve("result for ab")

    await expect(Promise.all([first, second])).resolves.toEqual([
      "result for ab",
      "result for ab",
    ])
    expect(events.started).toHaveBeenCalledTimes(2)
    expect(events.succeeded).toHaveBeenCalledExactlyOnceWith("result for ab")
  })

  it("aborts the request in flight on a new call and ignores its late result", async () => {
    const { runner, calls, events } = setup(0)

    runner.run("old")
    const latest = runner.run("new")
    calls[0]!.resolve("old result")
    calls[1]!.resolve("new result")

    expect(calls[0]!.signal.aborted).toBe(true)
    await expect(latest).resolves.toBe("new result")
    expect(events.succeeded).toHaveBeenCalledExactlyOnceWith("new result")
  })

  it("reports failures of the latest call and rejects its callers", async () => {
    const { runner, calls, events } = setup(0)
    const failure = new Error("boom")

    const result = runner.run("x")
    calls[0]!.reject(failure)

    await expect(result).rejects.toBe(failure)
    expect(events.failed).toHaveBeenCalledExactlyOnceWith(failure)
  })

  it("cancel() aborts, rejects callers with an AbortError and reports cancelled", async () => {
    const { runner, calls, events } = setup(0)

    const result = runner.run("x")
    runner.cancel()
    calls[0]!.resolve("too late")

    expect(calls[0]!.signal.aborted).toBe(true)
    expect(isAbortError(await result.catch((e: unknown) => e))).toBe(true)
    expect(events.cancelled).toHaveBeenCalledOnce()
    expect(events.succeeded).not.toHaveBeenCalled()
  })

  it("cancel() during the delay never sends", () => {
    const { runner, calls } = setup()

    runner.run("x").catch(() => undefined)
    runner.cancel()
    vi.runAllTimers()

    expect(calls).toHaveLength(0)
  })

  it("dispose() stops silently and the runner keeps working afterwards", async () => {
    const { runner, calls, events } = setup(0)

    const dropped = runner.run("x")
    runner.dispose()
    const next = runner.run("y")
    calls[1]!.resolve("y result")

    expect(isAbortError(await dropped.catch((e: unknown) => e))).toBe(true)
    expect(events.cancelled).not.toHaveBeenCalled()
    await expect(next).resolves.toBe("y result")
  })
})

describe("restart()", () => {
  it("repeats the last arguments right away, without the debounce", async () => {
    const { runner, calls } = setup()

    runner.run("query").catch(() => undefined)
    vi.advanceTimersByTime(300)
    calls[0]!.reject(new Error("offline"))
    await Promise.resolve()

    const retry = runner.restart()
    expect(calls).toHaveLength(2) // sent at once, no 300 ms wait
    expect(calls[1]!.args).toEqual(["query"])

    calls[1]!.resolve("back online")
    await expect(retry).resolves.toBe("back online")
  })

  it("aborts the request in flight like any new call", () => {
    const { runner, calls } = setup(0)

    runner.run("query")
    runner.restart()

    expect(calls[0]!.signal.aborted).toBe(true)
    expect(calls).toHaveLength(2)
  })

  it("does nothing before the first call", () => {
    const { runner, calls } = setup(0)

    expect(runner.restart()).toBeUndefined()
    expect(calls).toHaveLength(0)
  })

  it("does nothing after reset()", () => {
    const { runner, calls, events } = setup(0)

    runner.run("query").catch(() => undefined)
    runner.reset()

    expect(runner.restart()).toBeUndefined()
    expect(calls).toHaveLength(1)
    expect(events.cancelled).toHaveBeenCalledOnce()
  })
})
