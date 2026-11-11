import { z } from "zod"

import type { AuthErrorsTranslator } from "@/features/auth/lib/api-errors"
import { createFieldRules } from "@/features/auth/lib/validation"

/** The backend's limit for first and last names */
export const NAME_MAX_LENGTH = 100

export const SIGNUP_FIELDS = ["first_name", "last_name", "email"] as const

type SignupLabels = Record<"firstName" | "lastName" | "email", string>

/** Step 1 fields, named like the API body (snake_case) so server errors map 1:1 */
export function createSignupSchema(
  labels: SignupLabels,
  t: AuthErrorsTranslator
) {
  const rules = createFieldRules(t)
  return z.object({
    first_name: rules.text(labels.firstName, NAME_MAX_LENGTH),
    last_name: rules.text(labels.lastName, NAME_MAX_LENGTH),
    email: rules.email(labels.email),
  })
}

export type SignupValues = z.infer<ReturnType<typeof createSignupSchema>>
