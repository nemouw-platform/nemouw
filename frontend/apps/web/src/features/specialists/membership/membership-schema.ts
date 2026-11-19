import type { FormErrorsTranslator } from "@/lib/forms/api-errors"
import { createFieldRules } from "@/lib/forms/validation"

// The backend's rules (openapi.yaml `Username`, ValidationLimits.TEXT_MAX)
export const USERNAME_MIN_LENGTH = 3
export const USERNAME_MAX_LENGTH = 30
const USERNAME_PATTERN = /^[A-Za-z0-9._-]+$/
const MEMBER_NUMBER_MAX_LENGTH = 100

/** The membership fields, named like the API body so server errors map 1:1 */
export const MEMBERSHIP_FIELDS = [
  "username",
  "syndicate_id",
  "syndicate_member_number",
] as const

export type MembershipField = (typeof MEMBERSHIP_FIELDS)[number]

interface MembershipLabels {
  username: string
  syndicate: string
  memberNumber: string
}

/**
 * The rules for a specialist's username, syndicate and member number, used
 * where they are first set (onboarding) and changed (settings)
 */
export function membershipRules(
  labels: MembershipLabels,
  t: FormErrorsTranslator,
  usernameFormatMessage: string
) {
  const rules = createFieldRules(t)
  return {
    username: rules
      .text(labels.username, USERNAME_MAX_LENGTH)
      .min(USERNAME_MIN_LENGTH, t("tooShort", { min: USERNAME_MIN_LENGTH }))
      .regex(USERNAME_PATTERN, usernameFormatMessage),
    syndicate_id: rules.choice(labels.syndicate),
    syndicate_member_number: rules.text(
      labels.memberNumber,
      MEMBER_NUMBER_MAX_LENGTH
    ),
  }
}
