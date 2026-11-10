import { isApiError, type FieldErrorCode } from "@workspace/api"

import { pickFieldErrors } from "@/features/auth/lib/api-errors"
import {
  LOGIN_FIELDS,
  type LoginValues,
} from "@/features/auth/login/login-schema"

export type LoginFailure =
  | {
      kind: "fields"
      fields: Array<{ name: keyof LoginValues; code: FieldErrorCode }>
    }
  /** Wrong password, unknown email or deleted account: deliberately one answer */
  | { kind: "invalidCredentials" }
  /** Right password, but the account is suspended */
  | { kind: "suspended" }
  /** Right password, but the email was never confirmed */
  | { kind: "notVerified" }
  | { kind: "general" }

const BY_CODE: Partial<Record<string, LoginFailure>> = {
  INVALID_CREDENTIALS: { kind: "invalidCredentials" },
  ACCOUNT_SUSPENDED: { kind: "suspended" },
  ACCOUNT_NOT_VERIFIED: { kind: "notVerified" },
}

/** Classify a login error; pure, so it is easy to test */
export function classifyLoginError(error: unknown): LoginFailure {
  const known = isApiError(error) ? BY_CODE[error.code] : undefined
  if (known) return known
  const fields = pickFieldErrors(error, LOGIN_FIELDS)
  return fields.length > 0 ? { kind: "fields", fields } : { kind: "general" }
}
