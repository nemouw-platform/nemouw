import { z } from "zod"

import type { AuthErrorsTranslator } from "@/features/auth/lib/api-errors"
import { createFieldRules } from "@/features/auth/lib/validation"

/** Named like the API body, so server field errors map 1:1 */
export const RESET_FIELDS = ["new_password"] as const

export function createResetSchema(
  labels: { password: string },
  t: AuthErrorsTranslator
) {
  return z.object({
    new_password: createFieldRules(t).password(labels.password),
  })
}

export type ResetValues = z.infer<ReturnType<typeof createResetSchema>>
