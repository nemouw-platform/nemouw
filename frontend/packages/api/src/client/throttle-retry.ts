import type { ApiError } from "../errors"
import { isAuthPath } from "./session-refresh"

/** Retrying calls the backend's rate limiter turned away (429) */
export interface ThrottleRetry {
  /** Retries per call, after the first try */
  attempts: number
  /** A longer Retry-After is a real limit to show, not a bump to wait out */
  maxWaitSeconds: number
}

export const DEFAULT_THROTTLE_RETRY: ThrottleRetry = {
  attempts: 2,
  maxWaitSeconds: 5,
}

/** Wait used when a 429 has no Retry-After */
const DEFAULT_WAIT_SECONDS = 1

/**
 * Seconds to wait before trying a 429'd call again, or null to give up.
 * Only short waits are taken, and never for /auth/ calls: their 429s are
 * rules the screen explains ("you can resend the code in 60 seconds").
 */
export function throttleWait(
  error: ApiError,
  path: string,
  attempt: number,
  policy: ThrottleRetry | false
): number | null {
  if (!policy || error.status !== 429 || isAuthPath(path)) return null
  if (attempt >= policy.attempts) return null
  const wait = error.retryAfter ?? DEFAULT_WAIT_SECONDS
  return wait <= policy.maxWaitSeconds ? wait : null
}

/** Resolves after `ms`, or rejects like fetch (AbortError) if `signal` aborts */
export function sleep(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    const aborted = () =>
      signal?.reason ?? new DOMException("Aborted", "AbortError")
    if (signal?.aborted) return reject(aborted())
    const timer = setTimeout(() => {
      signal?.removeEventListener("abort", onAbort)
      resolve()
    }, ms)
    function onAbort() {
      clearTimeout(timer)
      reject(aborted())
    }
    signal?.addEventListener("abort", onAbort, { once: true })
  })
}
