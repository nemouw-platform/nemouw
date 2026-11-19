import { isApiError, type FieldErrorCode } from "@workspace/api"

import type { MembershipField } from "@/features/specialists/membership/membership-schema"
import { pickFieldErrors } from "@/lib/forms/api-errors"

export type MembershipFailure =
  | {
      kind: "fields"
      fields: Array<{ name: MembershipField; code: FieldErrorCode }>
    }
  | { kind: "usernameTaken" }
  /** The username passed the format but is reserved (e.g. "admin", "nemou…") */
  | { kind: "usernameUnavailable" }
  | { kind: "memberNumberTaken" }
  /** The syndicate cannot change once the profile is verified */
  | { kind: "syndicateLocked" }
  /** Onboarding was already done, e.g. in another tab */
  | { kind: "alreadyOnboarded" }
  | { kind: "general" }

const BY_CODE: Partial<Record<string, MembershipFailure>> = {
  USERNAME_TAKEN: { kind: "usernameTaken" },
  MEMBER_NUMBER_TAKEN: { kind: "memberNumberTaken" },
  SYNDICATE_LOCKED: { kind: "syndicateLocked" },
  PROFILE_ALREADY_EXISTS: { kind: "alreadyOnboarded" },
}

/**
 * Classify an error from setting or changing the membership fields (`fields`:
 * the ones the form has); pure, so it is easy to test
 */
export function classifyMembershipError(
  error: unknown,
  fields: readonly MembershipField[]
): MembershipFailure {
  const known = isApiError(error) ? BY_CODE[error.code] : undefined
  if (known) return known

  const found = pickFieldErrors(error, fields)
  // The form already enforces the username's format, so a format error from
  // the server on it means the backend's reserved-word list said no
  const reserved = found.some(
    ({ name, code }) => name === "username" && code === "INVALID_FORMAT"
  )
  if (reserved) return { kind: "usernameUnavailable" }
  return found.length > 0
    ? { kind: "fields", fields: found }
    : { kind: "general" }
}
