import { ApiError, type ApiErrorBody } from "@workspace/api"

/** Test helper: an ApiError as the client would throw it */
export function apiError(
  status: number,
  code: string,
  extra: Partial<ApiErrorBody> = {},
  retryAfter: number | null = null
): ApiError {
  return new ApiError({ status, code, message: code, ...extra }, retryAfter)
}
