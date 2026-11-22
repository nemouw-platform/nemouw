import type {
  ApiLocale,
  Gender,
  SpecialistResponse,
  SpecialistUpdateRequest,
} from "@workspace/api"
import { z } from "zod"

import { isPhoneNumber } from "@/components/form/phone-input/phone"
import type { FormErrorsTranslator } from "@/lib/forms/api-errors"
import { createFieldRules, parseAmount } from "@/lib/forms/validation"

/** Named like the API body, so server field errors map 1:1 */
export const DETAILS_FIELDS = [
  "years_of_experience",
  "gender",
  "primary_language",
  "whatsapp_number",
  "min_age_treated",
  "max_age_treated",
  "session_price_min",
  "session_price_max",
] as const

/** Stored as free text by the backend; these are the values we offer */
export const GENDERS = ["female", "male"] as const satisfies readonly Gender[]
/** Locale codes, so search can match them against the site's languages */
export const PRIMARY_LANGUAGES = [
  "ar",
  "en",
  "fr",
] as const satisfies readonly ApiLocale[]

// Sanity caps (the backend only asks for 0 or more)
const MAX_YEARS_OF_EXPERIENCE = 70
const MAX_AGE = 120
/** The backend's PriceRangeUpdate.MAX_PRICE */
const MAX_PRICE = 99_999_999.99
interface DetailsMessages {
  phoneFormat: string
  ageRange: string
  /** An "up to" price without a "from" price */
  priceNeedsMin: string
  /** An "up to" price below the "from" price */
  priceRange: string
}

export function createDetailsSchema(
  t: FormErrorsTranslator,
  messages: DetailsMessages
) {
  const rules = createFieldRules(t)
  return z
    .object({
      years_of_experience: rules.optionalCount(MAX_YEARS_OF_EXPERIENCE),
      // "" while not picked
      gender: z.enum(["", ...GENDERS]),
      primary_language: z.enum(["", ...PRIMARY_LANGUAGES]),
      // International ("+96170123456"), as PhoneInput gives it
      whatsapp_number: z
        .string()
        .refine((value) => value === "" || isPhoneNumber(value), {
          message: messages.phoneFormat,
        }),
      min_age_treated: rules.optionalCount(MAX_AGE),
      max_age_treated: rules.optionalCount(MAX_AGE),
      // A "from" price alone is fine; "up to" needs it
      session_price_min: rules.optionalAmount(MAX_PRICE),
      session_price_max: rules.optionalAmount(MAX_PRICE),
    })
    .refine(
      ({ min_age_treated: min, max_age_treated: max }) =>
        min === "" || max === "" || Number(min) <= Number(max),
      { path: ["max_age_treated"], message: messages.ageRange }
    )
    .refine(
      ({ session_price_min: min, session_price_max: max }) =>
        max === "" || min !== "",
      { path: ["session_price_max"], message: messages.priceNeedsMin }
    )
    .refine(
      ({ session_price_min: min, session_price_max: max }) =>
        min === "" || max === "" || parseAmount(min) <= parseAmount(max),
      { path: ["session_price_max"], message: messages.priceRange }
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
    gender: specialist.gender ?? "",
    primary_language: specialist.primary_language ?? "",
    whatsapp_number: text(specialist.whatsapp_number),
    min_age_treated: count(specialist.min_age_treated),
    max_age_treated: count(specialist.max_age_treated),
    session_price_min: count(specialist.session_price_min),
    session_price_max: count(specialist.session_price_max),
  }
}

const orNull = (value: string) => (value === "" ? null : value)
const countOrNull = (value: string) => (value === "" ? null : Number(value))
const amountOrNull = (value: string) =>
  value === "" ? null : parseAmount(value)

/** The PATCH body: every field sent, an empty one as null (clears it) */
export function toUpdateRequest(
  values: DetailsValues
): SpecialistUpdateRequest {
  return {
    years_of_experience: countOrNull(values.years_of_experience),
    gender: values.gender || null,
    primary_language: values.primary_language || null,
    whatsapp_number: orNull(values.whatsapp_number),
    min_age_treated: countOrNull(values.min_age_treated),
    max_age_treated: countOrNull(values.max_age_treated),
    // The currency is the backend's (USD for now): not sent
    session_price_min: amountOrNull(values.session_price_min),
    session_price_max: amountOrNull(values.session_price_max),
  }
}
