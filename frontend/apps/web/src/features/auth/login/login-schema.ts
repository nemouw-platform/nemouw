import { z } from "zod"

import type { AuthErrorsTranslator } from "@/features/auth/lib/api-errors"
import { createFieldRules } from "@/features/auth/lib/validation"

/** Named like the API body, so server field errors map 1:1 */
export const LOGIN_FIELDS = ["identifier", "password"] as const

/**
 * The API's `identifier` accepts email, phone or username, but only email
 * login exists today (docs/backend/user/README.md), so the form asks for an email.
 */
export function createLoginSchema(
  labels: { email: string; password: string },
  t: AuthErrorsTranslator
) {
  const rules = createFieldRules(t)
  return z.object({
    identifier: rules.email(labels.email),
    password: rules.secret(labels.password),
  })
}

export type LoginValues = z.infer<ReturnType<typeof createLoginSchema>>
