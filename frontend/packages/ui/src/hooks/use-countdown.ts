"use client"

import * as React from "react"

const TICK_MS = 250

/**
 * Seconds left until a deadline, e.g. "Resend code in 0:42". Counts from the
 * deadline rather than decrementing, so throttled timers in background tabs
 * never drift.
 *
 * @param initialSeconds start counting as soon as the component mounts
 */
export function useCountdown(initialSeconds = 0) {
  const [remaining, setRemaining] = React.useState(initialSeconds)
  const endsAt = React.useRef(0)
  const isRunning = remaining > 0

  // The clock is read on mount, not during render (prerendering forbids it)
  React.useEffect(() => {
    if (initialSeconds > 0) endsAt.current = Date.now() + initialSeconds * 1000
    // Only the first value matters; later ones go through start()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  React.useEffect(() => {
    if (!isRunning) return
    const id = setInterval(() => {
      setRemaining(Math.max(0, Math.ceil((endsAt.current - Date.now()) / 1000)))
    }, TICK_MS)
    return () => clearInterval(id)
  }, [isRunning])

  const start = React.useCallback((seconds: number) => {
    endsAt.current = Date.now() + seconds * 1000
    setRemaining(Math.max(0, Math.ceil(seconds)))
  }, [])

  return { remaining, isRunning, start }
}

/** 75 -> "1:15", for countdown labels */
export function formatCountdown(seconds: number): string {
  const minutes = Math.floor(seconds / 60)
  return `${minutes}:${String(seconds % 60).padStart(2, "0")}`
}
