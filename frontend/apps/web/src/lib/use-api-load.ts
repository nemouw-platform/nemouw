"use client"

import {
  useLoad,
  type UseLoadOptions,
  type UseLoadResult,
} from "@workspace/ui/hooks/use-load"

import { isTooManyRequests, retryAfterSeconds } from "@/lib/forms/api-errors"

/** A 429 is waited out (its Retry-After); any other failure is shown */
function throttleDelay(error: unknown): number | null {
  return isTooManyRequests(error) ? retryAfterSeconds(error) * 1000 : null
}

/**
 * useLoad for backend calls. The backend rate-limits per IP, so quick page
 * changes can get a 429: the load then waits as long as the server asks and
 * tries again, instead of showing an error.
 */
export function useApiLoad<TData>(
  load: (signal: AbortSignal) => Promise<TData>,
  options: UseLoadOptions<TData, unknown> = {}
): UseLoadResult<TData, unknown> {
  return useLoad(load, { retryDelay: throttleDelay, ...options })
}
