import { z } from "zod"

import type { AuthErrorsTranslator } from "@/features/auth/lib/api-errors"
import { createFieldRules } from "@/features/auth/lib/validation"

export function createForgotSchema(
  labels: { email: string },
  t: AuthErrorsTranslator
) {
  return z.object({ email: createFieldRules(t).email(labels.email) })
}

export type ForgotValues = z.infer<ReturnType<typeof createForgotSchema>>
