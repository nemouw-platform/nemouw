import { isApiError, type FieldErrorCode } from "@workspace/api"

import { pickFieldErrors } from "@/features/auth/lib/api-errors"
import { PASSWORD_FIELDS } from "@/features/auth/password/password-schema"

export type PasswordFailure =
  | {
      kind: "fields"
      fields: Array<{ name: "password"; code: FieldErrorCode }>
    }
  /** The 15-minute setup cookie is missing, expired or already used */
  | { kind: "setupExpired" }
  | { kind: "general" }

/**
 * Classify a password-setup error; pure, so it is easy to test. The backend
 * answers 401 when the setup cookie is gone, expired or spent (the account is
 * no longer pending), and 403 when the cookie is a normal session instead.
 */
export function classifyPasswordError(error: unknown): PasswordFailure {
  if (isApiError(error) && (error.status === 401 || error.status === 403)) {
    return { kind: "setupExpired" }
  }
  const fields = pickFieldErrors(error, PASSWORD_FIELDS)
  return fields.length > 0 ? { kind: "fields", fields } : { kind: "general" }
}
