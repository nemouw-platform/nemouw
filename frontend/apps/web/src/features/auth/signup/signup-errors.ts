import { isApiError, type FieldErrorCode } from "@workspace/api"

import {
  isTooManyRequests,
  pickFieldErrors,
  retryAfterSeconds,
} from "@/features/auth/lib/api-errors"
import {
  SIGNUP_FIELDS,
  type SignupValues,
} from "@/features/auth/signup/signup-schema"

/** What the form should show for a failed sign-up request; anything else is general */
export type SignupFailure =
  | {
      kind: "fields"
      fields: Array<{ name: keyof SignupValues; code: FieldErrorCode }>
    }
  | { kind: "emailTaken" }
  | { kind: "codeAlreadySent"; retryAfterSeconds: number }
  | { kind: "general" }

/** Classify a sign-up error; pure, so it is easy to test */
export function classifySignupError(error: unknown): SignupFailure {
  if (isApiError(error) && error.code === "EMAIL_TAKEN") {
    return { kind: "emailTaken" }
  }
  if (isTooManyRequests(error)) {
    // The email is still pending and a code went out under a minute ago
    return {
      kind: "codeAlreadySent",
      retryAfterSeconds: retryAfterSeconds(error),
    }
  }
  const fields = pickFieldErrors(error, SIGNUP_FIELDS)
  return fields.length > 0 ? { kind: "fields", fields } : { kind: "general" }
}
