import { z } from "zod"

import type { FormErrorsTranslator } from "@/lib/forms/api-errors"
import { createFieldRules } from "@/lib/forms/validation"

export function createForgotSchema(
  labels: { email: string },
  t: FormErrorsTranslator
) {
  return z.object({ email: createFieldRules(t).email(labels.email) })
}

export type ForgotValues = z.infer<ReturnType<typeof createForgotSchema>>
