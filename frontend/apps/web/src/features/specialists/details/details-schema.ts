import type {
  SpecialistResponse,
  SpecialistUpdateRequest,
} from "@workspace/api"
import { z } from "zod"

import { isPhoneNumber } from "@/components/form/phone-input/phone"
import type { FormErrorsTranslator } from "@/lib/forms/api-errors"
import { createFieldRules } from "@/lib/forms/validation"

/** Named like the API body, so server field errors map 1:1 */
export const DETAILS_FIELDS = [
  "years_of_experience",
  "gender",
  "primary_language",
  "whatsapp_number",
  "min_age_treated",
  "max_age_treated",
] as const

/** Stored as free text by the backend; these are the values we offer */
export const GENDERS = ["female", "male"] as const
/** Locale codes, so search can match them against the site's languages */
export const PRIMARY_LANGUAGES = ["ar", "en", "fr"] as const

// Sanity caps (the backend only asks for 0 or more)
const MAX_YEARS_OF_EXPERIENCE = 70
const MAX_AGE = 120
interface DetailsMessages {
  phoneFormat: string
  ageRange: string
}

export function createDetailsSchema(
  t: FormErrorsTranslator,
  messages: DetailsMessages
) {
  const rules = createFieldRules(t)
  return z
    .object({
      years_of_experience: rules.optionalCount(MAX_YEARS_OF_EXPERIENCE),
      gender: z.string(),
      primary_language: z.string(),
      // International ("+96170123456"), as PhoneInput gives it
      whatsapp_number: z
        .string()
        .refine((value) => value === "" || isPhoneNumber(value), {
          message: messages.phoneFormat,
        }),
      min_age_treated: rules.optionalCount(MAX_AGE),
      max_age_treated: rules.optionalCount(MAX_AGE),
    })
    .refine(
      ({ min_age_treated: min, max_age_treated: max }) =>
        min === "" || max === "" || Number(min) <= Number(max),
      { path: ["max_age_treated"], message: messages.ageRange }
    )
}

/** The form's values: text as typed, "" for an empty field */
export type DetailsValues = z.infer<ReturnType<typeof createDetailsSchema>>

const text = (value: string | null | undefined) => value ?? ""
const count = (value: number | null | undefined) =>
  value == null ? "" : String(value)

/** The saved profile as form values */
export function toDetailsValues(specialist: SpecialistResponse): DetailsValues {
  return {
    years_of_experience: count(specialist.years_of_experience),
    gender: text(specialist.gender),
    primary_language: text(specialist.primary_language),
    whatsapp_number: text(specialist.whatsapp_number),
    min_age_treated: count(specialist.min_age_treated),
    max_age_treated: count(specialist.max_age_treated),
  }
}

const orNull = (value: string) => (value === "" ? null : value)
const countOrNull = (value: string) => (value === "" ? null : Number(value))

/** The PATCH body: every field sent, an empty one as null (clears it) */
export function toUpdateRequest(
  values: DetailsValues
): SpecialistUpdateRequest {
  return {
    years_of_experience: countOrNull(values.years_of_experience),
    gender: orNull(values.gender),
    primary_language: orNull(values.primary_language),
    whatsapp_number: orNull(values.whatsapp_number),
    min_age_treated: countOrNull(values.min_age_treated),
    max_age_treated: countOrNull(values.max_age_treated),
  }
}
