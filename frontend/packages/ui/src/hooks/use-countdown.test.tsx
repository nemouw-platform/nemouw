// @vitest-environment jsdom
import { act, renderHook } from "@testing-library/react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import {
  formatCountdown,
  useCountdown,
} from "@workspace/ui/hooks/use-countdown"

beforeEach(() => vi.useFakeTimers())
afterEach(() => vi.useRealTimers())

describe("useCountdown", () => {
  it("is idle until started", () => {
    const { result } = renderHook(() => useCountdown())

    expect(result.current).toMatchObject({ remaining: 0, isRunning: false })
  })

  it("counts down to zero and stops", () => {
    const { result } = renderHook(() => useCountdown())

    act(() => result.current.start(3))
    expect(result.current).toMatchObject({ remaining: 3, isRunning: true })

    act(() => void vi.advanceTimersByTime(1000))
    expect(result.current.remaining).toBe(2)

    act(() => void vi.advanceTimersByTime(2000))
    expect(result.current).toMatchObject({ remaining: 0, isRunning: false })
  })

  it("can start counting on mount", () => {
    const { result } = renderHook(() => useCountdown(60))
    expect(result.current).toMatchObject({ remaining: 60, isRunning: true })

    act(() => void vi.advanceTimersByTime(15_000))
    expect(result.current.remaining).toBe(45)
  })

  it("restarting replaces the deadline", () => {
    const { result } = renderHook(() => useCountdown())

    act(() => result.current.start(10))
    act(() => void vi.advanceTimersByTime(8000))
    act(() => result.current.start(60))

    expect(result.current.remaining).toBe(60)
  })

  it("follows the clock even if ticks are delayed", () => {
    const { result } = renderHook(() => useCountdown())

    act(() => result.current.start(30))
    act(() => {
      vi.setSystemTime(Date.now() + 20_000) // a throttled tab skipped ticks
      vi.advanceTimersByTime(250)
    })

    expect(result.current.remaining).toBe(10)
  })
})

describe("formatCountdown", () => {
  it.each([
    [0, "0:00"],
    [9, "0:09"],
    [60, "1:00"],
    [75, "1:15"],
  ])("formats %i as %s", (seconds, label) => {
    expect(formatCountdown(seconds)).toBe(label)
  })
})
