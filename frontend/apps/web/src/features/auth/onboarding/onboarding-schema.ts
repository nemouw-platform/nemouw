import { z } from "zod"

import type { AuthErrorsTranslator } from "@/features/auth/lib/api-errors"
import { createFieldRules } from "@/features/auth/lib/validation"

// The backend's rules (openapi.yaml `Username`, ValidationLimits.TEXT_MAX)
export const USERNAME_MIN_LENGTH = 3
export const USERNAME_MAX_LENGTH = 30
const USERNAME_PATTERN = /^[A-Za-z0-9._-]+$/
const MEMBER_NUMBER_MAX_LENGTH = 100

/** Named like the API body, so server field errors map 1:1 */
export const ONBOARDING_FIELDS = [
  "username",
  "syndicate_id",
  "syndicate_member_number",
] as const

type OnboardingLabels = Record<
  "username" | "syndicate" | "memberNumber",
  string
>

export function createOnboardingSchema(
  labels: OnboardingLabels,
  t: AuthErrorsTranslator,
  usernameFormatMessage: string
) {
  const rules = createFieldRules(t)
  return z.object({
    username: rules
      .text(labels.username, USERNAME_MAX_LENGTH)
      .min(USERNAME_MIN_LENGTH, t("tooShort", { min: USERNAME_MIN_LENGTH }))
      .regex(USERNAME_PATTERN, usernameFormatMessage),
    syndicate_id: rules.choice(labels.syndicate),
    syndicate_member_number: rules.text(
      labels.memberNumber,
      MEMBER_NUMBER_MAX_LENGTH
    ),
  })
}

export type OnboardingValues = z.infer<
  ReturnType<typeof createOnboardingSchema>
>
