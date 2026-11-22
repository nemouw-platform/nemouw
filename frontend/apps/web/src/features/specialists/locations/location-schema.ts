import type {
  SpecialistLocation,
  SpecialistLocationWriteRequest,
} from "@workspace/api"
import { z } from "zod"

import { isPhoneNumber } from "@/components/form/phone-input/phone"
import type { FormErrorsTranslator } from "@/lib/forms/api-errors"
import { createFieldRules } from "@/lib/forms/validation"

/** Named like the API body, so server field errors map 1:1 */
export const LOCATION_FIELDS = [
  "label",
  "country_id",
  "city",
  "address_line1",
  "address_line2",
  "phone",
] as const

/** The backend's ValidationLimits.TEXT_MAX */
const TEXT_MAX_LENGTH = 100

export function createLocationSchema(
  labels: { country: string; city: string },
  t: FormErrorsTranslator,
  messages: { phoneFormat: string }
) {
  const rules = createFieldRules(t)
  return z.object({
    label: rules.optionalText(TEXT_MAX_LENGTH),
    country_id: rules.choice(labels.country),
    city: rules.text(labels.city, TEXT_MAX_LENGTH),
    address_line1: rules.optionalText(TEXT_MAX_LENGTH),
    address_line2: rules.optionalText(TEXT_MAX_LENGTH),
    // International, as PhoneInput gives it
    phone: z.string().refine((value) => value === "" || isPhoneNumber(value), {
      message: messages.phoneFormat,
    }),
    is_primary: z.boolean(),
  })
}

export type LocationValues = z.infer<ReturnType<typeof createLocationSchema>>

/** A saved location as form values; a new one starts in `defaultCountryId` */
export function toLocationValues(
  location: SpecialistLocation | undefined,
  defaultCountryId: string
): LocationValues {
  return {
    label: location?.label ?? "",
    country_id: location?.country_id ?? defaultCountryId,
    city: location?.city ?? "",
    address_line1: location?.address_line1 ?? "",
    address_line2: location?.address_line2 ?? "",
    phone: location?.phone ?? "",
    is_primary: location?.is_primary ?? false,
  }
}

/**
 * The PUT/POST body: a full replacement where an empty field is left out
 * (stored as null). is_primary is only ever sent as true: leaving it out
 * keeps a location as it is, and the backend refuses to demote the primary.
 */
export function toLocationRequest(
  values: LocationValues
): SpecialistLocationWriteRequest {
  const optional = (
    ["label", "address_line1", "address_line2", "phone"] as const
  )
    .filter((field) => values[field] !== "")
    .map((field) => [field, values[field]])
  return {
    country_id: values.country_id,
    city: values.city,
    ...Object.fromEntries(optional),
    ...(values.is_primary && { is_primary: true }),
  }
}

/** The location as a request, made the primary one (for "Make main") */
export function asPrimaryRequest(
  location: SpecialistLocation
): SpecialistLocationWriteRequest {
  return toLocationRequest({
    ...toLocationValues(location, location.country_id ?? ""),
    is_primary: true,
  })
}
