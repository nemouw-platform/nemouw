import { isApiError, type FieldErrorCode } from "@workspace/api"

import { pickFieldErrors } from "@/features/auth/lib/api-errors"
import { RESET_FIELDS } from "@/features/auth/reset/reset-schema"

export type ResetFailure =
  | {
      kind: "fields"
      fields: Array<{ name: "new_password"; code: FieldErrorCode }>
    }
  /** Expired (60 min), already used, or not a real token */
  | { kind: "invalidLink" }
  | { kind: "general" }

/** Classify a password-reset error; pure, so it is easy to test */
export function classifyResetError(error: unknown): ResetFailure {
  const badToken =
    (isApiError(error) && error.code === "INVALID_OR_EXPIRED_TOKEN") ||
    pickFieldErrors(error, ["token"]).length > 0
  if (badToken) return { kind: "invalidLink" }
  const fields = pickFieldErrors(error, RESET_FIELDS)
  return fields.length > 0 ? { kind: "fields", fields } : { kind: "general" }
}
