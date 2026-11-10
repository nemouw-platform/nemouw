import { z } from "zod"

import type { AuthErrorsTranslator } from "@/features/auth/lib/api-errors"
import {
  containsPersonalInfo,
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  passwordLength,
} from "@/features/auth/lib/password-rules"

// Same limits the backend enforces (docs/backend/user/README.md, "Validation rules")
export const EMAIL_MAX_LENGTH = 254
/** The backend's email check: something@something.something, no spaces */
const EMAIL_SHAPE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * zod rules with the shared translated messages, so every auth form says
 * "{label} is required" (and the rest) the same way.
 */
export function createFieldRules(t: AuthErrorsTranslator) {
  const required = (label: string) => t("required", { field: label })

  return {
    /** Trimmed text that must not be blank */
    text: (label: string, maxLength: number) =>
      z
        .string()
        .trim()
        .min(1, required(label))
        .max(maxLength, t("tooLong", { max: maxLength })),

    email: (label: string) =>
      z
        .string()
        .trim()
        .min(1, required(label))
        .max(EMAIL_MAX_LENGTH, t("tooLong", { max: EMAIL_MAX_LENGTH }))
        .regex(EMAIL_SHAPE, t("field.MUST_BE_EMAIL")),

    /**
     * Required, sent exactly as typed: no trimming and no rules, e.g. the
     * password on the login form (an old password may predate today's rules)
     */
    secret: (label: string) => z.string().min(1, required(label)),

    /** The backend's password rules except the common-password list (server only) */
    password: (label: string, email?: string) =>
      z
        .string()
        .min(1, required(label))
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
        }),

    /** Exactly `length` digits, e.g. an emailed code */
    digits: (label: string, length: number) =>
      z
        .string()
        .min(1, required(label))
        .regex(new RegExp(`^\\d{${length}}$`), t("incompleteCode", { length })),
  }
}
