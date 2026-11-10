import { z } from "zod"

import type { AuthErrorsTranslator } from "@/features/auth/lib/api-errors"
import { OTP_LENGTH } from "@/features/auth/lib/constants"
import { createFieldRules } from "@/features/auth/lib/validation"

export function createVerifySchema(
  labels: { code: string },
  t: AuthErrorsTranslator
) {
  return z.object({
    code: createFieldRules(t).digits(labels.code, OTP_LENGTH),
  })
}

export type VerifyValues = z.infer<ReturnType<typeof createVerifySchema>>
