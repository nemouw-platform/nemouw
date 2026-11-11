import { isApiError, type FieldErrorCode } from "@workspace/api"

import { pickFieldErrors } from "@/features/auth/lib/api-errors"
import {
  ONBOARDING_FIELDS,
  type OnboardingValues,
} from "@/features/auth/onboarding/onboarding-schema"

export type OnboardingFailure =
  | {
      kind: "fields"
      fields: Array<{ name: keyof OnboardingValues; code: FieldErrorCode }>
    }
  | { kind: "usernameTaken" }
  /** The username passed the format but is reserved (e.g. "admin", "nemou…") */
  | { kind: "usernameUnavailable" }
  | { kind: "memberNumberTaken" }
  /** Onboarding was already done, e.g. in another tab */
  | { kind: "alreadyOnboarded" }
  | { kind: "general" }

const BY_CODE: Partial<Record<string, OnboardingFailure>> = {
  USERNAME_TAKEN: { kind: "usernameTaken" },
  MEMBER_NUMBER_TAKEN: { kind: "memberNumberTaken" },
  PROFILE_ALREADY_EXISTS: { kind: "alreadyOnboarded" },
}

/** Classify an onboarding error; pure, so it is easy to test */
export function classifyOnboardingError(error: unknown): OnboardingFailure {
  const known = isApiError(error) ? BY_CODE[error.code] : undefined
  if (known) return known

  const fields = pickFieldErrors(error, ONBOARDING_FIELDS)
  // The form already enforces the username's format, so a format error from
  // the server on it means the backend's reserved-word list said no
  const reserved = fields.some(
    ({ name, code }) => name === "username" && code === "INVALID_FORMAT"
  )
  if (reserved) return { kind: "usernameUnavailable" }
  return fields.length > 0 ? { kind: "fields", fields } : { kind: "general" }
}
