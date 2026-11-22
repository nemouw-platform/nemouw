import { passwordRule } from "@/features/auth/lib/validation"
import { z } from "zod"

import type { FormErrorsTranslator } from "@/lib/forms/api-errors"

/** Named like the API body, so server field errors map 1:1 */
export const RESET_FIELDS = ["new_password"] as const

export function createResetSchema(
  labels: { password: string },
  t: FormErrorsTranslator
) {
  return z.object({
    new_password: passwordRule(t, labels.password),
  })
}

export type ResetValues = z.infer<ReturnType<typeof createResetSchema>>
