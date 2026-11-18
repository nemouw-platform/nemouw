"use client"

import * as React from "react"

import {
  useRequest,
  type UseRequestOptions,
} from "@workspace/ui/hooks/use-request"

export interface UseLoadResult<TData, TError> {
  /** The loaded data; undefined until the first load succeeds */
  data: TData | undefined
  error: TError | undefined
  /** No data yet and the load is running (also true before it starts) */
  isLoading: boolean
  /** The latest load failed; offer `retry` */
  failed: boolean
  /** Load again right away, e.g. a "Try again" button */
  retry: () => void
}

/**
 * Loads data once when the component mounts, through useRequest (so it is
 * aborted on unmount). Pass the signal on: `useLoad((signal) => api.list({ signal }))`.
 */
export function useLoad<TData, TError = unknown>(
  load: (signal: AbortSignal) => Promise<TData>,
  options: Omit<UseRequestOptions<TData, TError>, "debounceMs"> = {}
): UseLoadResult<TData, TError> {
  const { data, error, status, run, restart } = useRequest<[], TData, TError>(
    load,
    { ...options, debounceMs: 0 }
  )

  React.useEffect(() => run(), [run])

  return {
    data,
    error,
    isLoading: status === "idle" || status === "pending",
    failed: status === "error",
    retry: restart,
  }
}
