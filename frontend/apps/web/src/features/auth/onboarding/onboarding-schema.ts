import { z } from "zod"

import {
  MEMBERSHIP_FIELDS,
  membershipRules,
} from "@/features/specialists/membership/membership-schema"
import type { FormErrorsTranslator } from "@/lib/forms/api-errors"

/** Onboarding sets every membership field at once */
export const ONBOARDING_FIELDS = MEMBERSHIP_FIELDS

export function createOnboardingSchema(
  labels: Parameters<typeof membershipRules>[0],
  t: FormErrorsTranslator,
  usernameFormatMessage: string
) {
  return z.object(membershipRules(labels, t, usernameFormatMessage))
}

export type OnboardingValues = z.infer<
  ReturnType<typeof createOnboardingSchema>
>
