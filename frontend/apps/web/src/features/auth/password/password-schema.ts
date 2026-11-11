import { z } from "zod"

import type { AuthErrorsTranslator } from "@/features/auth/lib/api-errors"
import { createFieldRules } from "@/features/auth/lib/validation"

export const PASSWORD_FIELDS = ["password"] as const

/** Step 3. `email` (when known) lets the form flag passwords that contain it */
export function createPasswordSchema(
  labels: { password: string },
  t: AuthErrorsTranslator,
  email?: string
) {
  return z.object({
    password: createFieldRules(t).password(labels.password, email),
  })
}

export type PasswordValues = z.infer<ReturnType<typeof createPasswordSchema>>
