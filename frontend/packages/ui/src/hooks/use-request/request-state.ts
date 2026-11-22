import type {
  RequestState,
  RequestStatus,
} from "@workspace/ui/hooks/use-request/types"

export type RequestAction<TData, TError> =
  | { type: "started" }
  | { type: "succeeded"; data: TData }
  | { type: "failed"; error: TError }
  | { type: "cancelled" }
  | { type: "reset" }

export function createInitialState<TData, TError>(): RequestState<
  TData,
  TError
> {
  return { status: "idle", data: undefined, error: undefined }
}

/** Pure state transitions; data survives a new call, error survives until a success */
export function requestReducer<TData, TError>(
  state: RequestState<TData, TError>,
  action: RequestAction<TData, TError>
): RequestState<TData, TError> {
  switch (action.type) {
    case "started":
      return state.status === "pending"
        ? state
        : { ...state, status: "pending" }
    case "succeeded":
      return { status: "success", data: action.data, error: undefined }
    case "failed":
      return { ...state, status: "error", error: action.error }
    case "cancelled":
      return state.status === "pending"
        ? { ...state, status: statusBeforePending(state) }
        : state
    case "reset":
      return createInitialState()
  }
}

/** What a dropped pending call falls back to */
function statusBeforePending<TData, TError>(
  state: RequestState<TData, TError>
): RequestStatus {
  if (state.error !== undefined) return "error"
  if (state.data !== undefined) return "success"
  return "idle"
}
