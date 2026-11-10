import { isApiError, type FieldErrorCode } from "@workspace/api"

import {
  SIGNUP_FIELDS,
  type SignupValues,
} from "@/features/auth/signup/signup-schema"

/** What the form should show for a failed sign-up request */
export type SignupFailure =
  | {
      kind: "fields"
      fields: Array<{ name: keyof SignupValues; code: FieldErrorCode }>
    }
  | { kind: "emailTaken" }
  | { kind: "codeAlreadySent"; retryAfterSeconds: number }
  | { kind: "general"; error: unknown }

/** Default wait when a 429 has no Retry-After (the backend's resend cooldown) */
const DEFAULT_RETRY_AFTER_SECONDS = 60

const isSignupField = (field: string): field is keyof SignupValues =>
  (SIGNUP_FIELDS as readonly string[]).includes(field)

/** Classify a sign-up error; pure, so it is easy to test */
export function classifySignupError(error: unknown): SignupFailure {
  if (!isApiError(error)) return { kind: "general", error }

  switch (error.code) {
    case "EMAIL_TAKEN":
      return { kind: "emailTaken" }
    case "TOO_MANY_REQUESTS":
      // The email is still pending and a code went out under a minute ago
      return {
        kind: "codeAlreadySent",
        retryAfterSeconds: error.retryAfter ?? DEFAULT_RETRY_AFTER_SECONDS,
      }
    case "VALIDATION_ERROR": {
      const fields = error.fieldErrors
        .filter((fieldError) => isSignupField(fieldError.field))
        .map((fieldError) => ({
          name: fieldError.field as keyof SignupValues,
          code: fieldError.code,
        }))
      return fields.length > 0
        ? { kind: "fields", fields }
        : { kind: "general", error }
    }
    default:
      return { kind: "general", error }
  }
}
