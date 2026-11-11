export type RequestStatus = "idle" | "pending" | "success" | "error"

/**
 * The async work to run. Its AbortSignal is aborted when a newer call replaces
 * this one, on cancel() / reset(), and on unmount: pass it on to fetch or the
 * API client so superseded requests are really cancelled.
 */
export type RequestFn<TArgs extends unknown[], TData> = (
  signal: AbortSignal,
  ...args: TArgs
) => Promise<TData>

export interface RequestState<TData, TError> {
  status: RequestStatus
  /** Result of the latest successful call; kept while a new call is pending */
  data: TData | undefined
  /** Error of the latest failed call; cleared by the next success */
  error: TError | undefined
}

export interface UseRequestOptions<TData, TError> {
  /**
   * Wait this long after the last call before sending, so rapid calls (typing,
   * double clicks) send one request. 0 sends immediately. Default: 300.
   */
  debounceMs?: number
  /** Called with the result of the latest call only */
  onSuccess?: (data: TData) => void
  /** Called with the error of the latest call only, never for aborts */
  onError?: (error: TError) => void
}

export interface UseRequestResult<
  TArgs extends unknown[],
  TData,
  TError,
> extends RequestState<TData, TError> {
  /** Waiting for the debounce or for the response */
  isPending: boolean
  /** Fire and forget (e.g. search as you type); errors only land in `error` */
  run: (...args: TArgs) => void
  /**
   * Resolves with the result of the call that wins (calls made in the same
   * debounce window share it) and rejects with its error. Rejects with an
   * AbortError on cancel(), reset() and unmount: check with isAbortError().
   */
  runAsync: (...args: TArgs) => Promise<TData>
  /**
   * Repeat the last call with the same arguments, right away (no debounce),
   * e.g. a "Try again" button. Errors land in `error`. Does nothing before the
   * first call or after reset().
   */
  restart: () => void
  /** Drop the pending debounce and abort the request in flight; keeps data and error */
  cancel: () => void
  /** cancel(), clear data and error, and forget the last arguments */
  reset: () => void
}
