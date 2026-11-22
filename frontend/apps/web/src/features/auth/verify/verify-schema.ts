import { OTP_LENGTH } from "@/features/auth/lib/constants"
import { z } from "zod"

import type { FormErrorsTranslator } from "@/lib/forms/api-errors"
import { createFieldRules } from "@/lib/forms/validation"

export function createVerifySchema(
  labels: { code: string },
  t: FormErrorsTranslator
) {
  return z.object({
    code: createFieldRules(t).digits(labels.code, OTP_LENGTH),
  })
}

export type VerifyValues = z.infer<ReturnType<typeof createVerifySchema>>
