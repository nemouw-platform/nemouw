import { isApiError } from "@workspace/api"

import { pickFieldErrors } from "@/features/auth/lib/api-errors"

/**
 * "invalidCode" for anything wrong with the code: the backend answers every
 * wrong, expired, used or over-tried code with the same 400 (so it reveals
 * nothing), and a malformed code with a 422 on `code`. Anything else is general.
 */
export function classifyVerifyError(error: unknown): "invalidCode" | "general" {
  const isRejectedCode =
    (isApiError(error) && error.code === "INVALID_OR_EXPIRED_TOKEN") ||
    pickFieldErrors(error, ["code"]).length > 0
  return isRejectedCode ? "invalidCode" : "general"
}
