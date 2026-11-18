"use client"

import * as React from "react"

import {
  useRequest,
  type UseRequestOptions,
} from "@workspace/ui/hooks/use-request"

export interface UseLoadOptions<TData, TError> extends Omit<
  UseRequestOptions<TData, TError>,
  "debounceMs"
> {
  /**
   * For failures worth waiting out (e.g. a 429 with Retry-After): how many ms
   * to wait before loading again on its own, or null to fail at once. It
   * keeps loading meanwhile, and gives up after a few tries. `onError` only
   * hears the failure it gives up on.
   */
  retryDelay?: (error: TError) => number | null
}

export interface UseLoadResult<TData, TError> {
  /** The loaded data; undefined until the first load succeeds */
  data: TData | undefined
  error: TError | undefined
  /** No data yet and the load is running, or waiting to retry (also true before it starts) */
  isLoading: boolean
  /** The latest load failed for good; offer `retry` */
  failed: boolean
  /** Load again right away, e.g. a "Try again" button */
  retry: () => void
}

/** Automatic retries, counting only those retryDelay asked for */
const MAX_AUTO_RETRIES = 3

/**
 * Loads data once when the component mounts, through useRequest (so it is
 * aborted on unmount). Pass the signal on: `useLoad((signal) => api.list({ signal }))`.
 */
export function useLoad<TData, TError = unknown>(
  load: (signal: AbortSignal) => Promise<TData>,
  {
    retryDelay,
    onError,
    onSuccess,
    ...options
  }: UseLoadOptions<TData, TError> = {}
): UseLoadResult<TData, TError> {
  const [waiting, setWaiting] = React.useState(false)
  const autoRetries = React.useRef(0)
  const timer = React.useRef<ReturnType<typeof setTimeout>>(undefined)

  const { data, error, status, run, restart } = useRequest<[], TData, TError>(
    load,
    {
      ...options,
      debounceMs: 0,
      onSuccess: (result) => {
        autoRetries.current = 0
        onSuccess?.(result)
      },
      onError: (failure) => {
        const delay = retryDelay?.(failure) ?? null
        if (delay === null || autoRetries.current >= MAX_AUTO_RETRIES) {
          onError?.(failure)
          return
        }
        autoRetries.current += 1
        setWaiting(true)
        timer.current = setTimeout(() => {
          setWaiting(false)
          restart()
        }, delay)
      },
    }
  )

  React.useEffect(() => run(), [run])
  React.useEffect(() => () => clearTimeout(timer.current), [])

  const retry = React.useCallback(() => {
    autoRetries.current = 0
    restart()
  }, [restart])

  return {
    data,
    error,
    isLoading: status === "idle" || status === "pending" || waiting,
    failed: status === "error" && !waiting,
    retry,
  }
}
