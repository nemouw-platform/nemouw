import { passwordRule } from "@/features/auth/lib/validation"
import { z } from "zod"

import type { FormErrorsTranslator } from "@/lib/forms/api-errors"

export const PASSWORD_FIELDS = ["password"] as const

/** Step 3. `email` (when known) lets the form flag passwords that contain it */
export function createPasswordSchema(
  labels: { password: string },
  t: FormErrorsTranslator,
  email?: string
) {
  return z.object({
    password: passwordRule(t, labels.password, email),
  })
}

export type PasswordValues = z.infer<ReturnType<typeof createPasswordSchema>>
