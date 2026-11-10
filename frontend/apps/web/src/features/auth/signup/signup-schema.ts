import type { useTranslations } from "next-intl"
import { z } from "zod"

// Same limits the backend enforces (docs/backend/user/README.md, "Validation rules")
export const NAME_MAX_LENGTH = 100
export const EMAIL_MAX_LENGTH = 254
/** The backend's email check: something@something.something, no spaces */
const EMAIL_SHAPE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export type SignupErrorsTranslator = ReturnType<
  typeof useTranslations<"Signup.errors">
>

/** Step 1 fields, named like the API body (snake_case) so server errors map 1:1 */
export function createSignupSchema(t: SignupErrorsTranslator) {
  const name = (requiredMessage: string) =>
    z
      .string()
      .trim()
      .min(1, requiredMessage)
      .max(NAME_MAX_LENGTH, t("nameTooLong", { max: NAME_MAX_LENGTH }))

  return z.object({
    first_name: name(t("firstNameRequired")),
    last_name: name(t("lastNameRequired")),
    email: z
      .string()
      .trim()
      .min(1, t("emailRequired"))
      .max(EMAIL_MAX_LENGTH, t("emailTooLong", { max: EMAIL_MAX_LENGTH }))
      .regex(EMAIL_SHAPE, t("emailInvalid")),
  })
}

export type SignupValues = z.infer<ReturnType<typeof createSignupSchema>>

export const SIGNUP_FIELDS = ["first_name", "last_name", "email"] as const
