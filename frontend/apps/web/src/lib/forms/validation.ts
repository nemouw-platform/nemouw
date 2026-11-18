import { z } from "zod"

import type { FormErrorsTranslator } from "@/lib/forms/api-errors"

// Same limits the backend enforces (docs/backend/user/README.md, "Validation rules")
export const EMAIL_MAX_LENGTH = 254
/** The backend's email check: something@something.something, no spaces */
const EMAIL_SHAPE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * zod rules with the shared translated messages, so every form says
 * "{label} is required" (and the rest) the same way.
 */
export function createFieldRules(t: FormErrorsTranslator) {
  const required = (label: string) => t("required", { field: label })

  return {
    /** Trimmed text that must not be blank */
    text: (label: string, maxLength: number) =>
      z
        .string()
        .trim()
        .min(1, required(label))
        .max(maxLength, t("tooLong", { max: maxLength })),

    /** Trimmed text that may be left blank (the form sends null for blank) */
    optionalText: (maxLength: number) =>
      z
        .string()
        .trim()
        .max(maxLength, t("tooLong", { max: maxLength })),

    /**
     * A whole number of at least 0 typed into a text box, or blank. Stays a
     * string: the form converts it when it builds the request body.
     */
    optionalCount: (max: number) =>
      z
        .string()
        .trim()
        .regex(/^\d*$/, t("wholeNumber"))
        .refine((value) => value === "" || Number(value) <= max, {
          message: t("tooLarge", { max }),
        }),

    email: (label: string) =>
      z
        .string()
        .trim()
        .min(1, required(label))
        .max(EMAIL_MAX_LENGTH, t("tooLong", { max: EMAIL_MAX_LENGTH }))
        .regex(EMAIL_SHAPE, t("field.MUST_BE_EMAIL")),

    /** A required pick from a list (its value, e.g. an id) */
    choice: (label: string) => z.string().min(1, required(label)),

    /**
     * Required, sent exactly as typed: no trimming and no rules, e.g. the
     * password on the login form (an old password may predate today's rules)
     */
    secret: (label: string) => z.string().min(1, required(label)),

    /** Exactly `length` digits, e.g. an emailed code */
    digits: (label: string, length: number) =>
      z
        .string()
        .min(1, required(label))
        .regex(new RegExp(`^\\d{${length}}$`), t("incompleteCode", { length })),
  }
}
