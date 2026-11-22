"use client"

import * as React from "react"

import { createRequestRunner } from "@workspace/ui/hooks/use-request/request-runner"
import {
  createInitialState,
  requestReducer,
} from "@workspace/ui/hooks/use-request/request-state"
import type {
  RequestFn,
  UseRequestOptions,
  UseRequestResult,
} from "@workspace/ui/hooks/use-request/types"

/** Default wait after the last call before a request is sent */
export const DEFAULT_DEBOUNCE_MS = 300

const ignore = () => undefined

/**
 * Runs async work (usually an API call) with debouncing, cancellation of
 * superseded calls, and loading / data / error state. Only the latest call can
 * update state, so out-of-order responses never show stale results.
 *
 * @example Search as you type
 * const search = useRequest((signal, q: string) =>
 *   api.get<Result[]>(`/specialists?q=${encodeURIComponent(q)}`, { signal }))
 * <input onChange={(e) => search.run(e.target.value)} />
 *
 * @example Form submit
 * const signup = useRequest((signal, body: SignupRequest) => auth.signup(body, { signal }))
 * try { await signup.runAsync(values) } catch (e) { if (!isAbortError(e)) showErrors(e) }
 */
export function useRequest<TArgs extends unknown[], TData, TError = unknown>(
  request: RequestFn<TArgs, TData>,
  options: UseRequestOptions<TData, TError> = {}
): UseRequestResult<TArgs, TData, TError> {
  const [state, dispatch] = React.useReducer(
    requestReducer<TData, TError>,
    undefined,
    createInitialState<TData, TError>
  )

  // Latest props, read by the runner at call time, so inline functions and
  // changing options never reset the debounce
  const latest = React.useRef({ request, options })
  React.useLayoutEffect(() => {
    latest.current = { request, options }
  })

  const [runner] = React.useState(() =>
    createRequestRunner<TArgs, TData>({
      getRequest: () => latest.current.request,
      getDelayMs: () =>
        latest.current.options.debounceMs ?? DEFAULT_DEBOUNCE_MS,
      events: {
        started: () => dispatch({ type: "started" }),
        succeeded: (data) => {
          dispatch({ type: "succeeded", data })
          latest.current.options.onSuccess?.(data)
        },
        failed: (error) => {
          dispatch({ type: "failed", error: error as TError })
          latest.current.options.onError?.(error as TError)
        },
        cancelled: () => dispatch({ type: "cancelled" }),
      },
    })
  )

  // dispose() leaves the runner usable, so Strict Mode's remount still works
  React.useEffect(() => () => runner.dispose(), [runner])

  // Errors are already in state; swallowing avoids unhandled rejections
  const run = React.useCallback(
    (...args: TArgs) => void runner.run(...args).catch(ignore),
    [runner]
  )

  const restart = React.useCallback(
    () => void runner.restart()?.catch(ignore),
    [runner]
  )

  const reset = React.useCallback(() => {
    runner.reset()
    dispatch({ type: "reset" })
  }, [runner])

  return {
    ...state,
    isPending: state.status === "pending",
    run,
    runAsync: runner.run,
    restart,
    cancel: runner.cancel,
    reset,
  }
}
