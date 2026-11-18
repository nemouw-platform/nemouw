import { z } from "zod"

import {
  containsPersonalInfo,
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  passwordLength,
} from "@/features/auth/lib/password-rules"
import type { FormErrorsTranslator } from "@/lib/forms/api-errors"

/**
 * The backend's password rules except the common-password list (server
 * only), with the shared translated messages
 */
export function passwordRule(
  t: FormErrorsTranslator,
  label: string,
  email?: string
) {
  return z
    .string()
    .min(1, t("required", { field: label }))
    .refine((value) => passwordLength(value) >= PASSWORD_MIN_LENGTH, {
      message: t("tooShort", { min: PASSWORD_MIN_LENGTH }),
      abort: true,
    })
    .refine((value) => passwordLength(value) <= PASSWORD_MAX_LENGTH, {
      message: t("tooLong", { max: PASSWORD_MAX_LENGTH }),
      abort: true,
    })
    .refine((value) => !containsPersonalInfo(value, email), {
      message: t("field.CONTAINS_PERSONAL_INFO"),
    })
}
