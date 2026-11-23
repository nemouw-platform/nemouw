import type { SyndicateResponse, SyndicateUpdateRequest } from "@workspace/api"
import { z } from "zod"

import { isPhoneNumber } from "@/components/form/phone-input/phone"
import type { FormErrorsTranslator } from "@/lib/forms/api-errors"
import { createFieldRules } from "@/lib/forms/validation"

/** Named like the API body, so server field errors map 1:1 */
export const SYNDICATE_FIELDS = [
  "name",
  "contact_email",
  "contact_phone",
] as const

/** The backend's ValidationLimits.NAME_MAX */
const NAME_MAX_LENGTH = 100

/**
 * The contact fields may be blank: GET /syndicates/{id} does not return
 * them yet (only a save does), and the PATCH keeps what it is not sent
 */
export function createSyndicateSchema(
  labels: { name: string },
  t: FormErrorsTranslator,
  messages: { phoneFormat: string }
) {
  const rules = createFieldRules(t)
  return z.object({
    name: rules.text(labels.name, NAME_MAX_LENGTH),
    contact_email: rules.optionalEmail(),
    // International, as PhoneInput gives it
    contact_phone: z
      .string()
      .refine((value) => value === "" || isPhoneNumber(value), {
        message: messages.phoneFormat,
      }),
  })
}

export type SyndicateValues = z.infer<ReturnType<typeof createSyndicateSchema>>

export function toSyndicateValues(
  syndicate: SyndicateResponse
): SyndicateValues {
  return {
    name: syndicate.name ?? "",
    contact_email: syndicate.contact_email ?? "",
    contact_phone: syndicate.contact_phone ?? "",
  }
}

/** A partial update: a blank contact field is left out, so it keeps its value */
export function toSyndicateRequest(
  values: SyndicateValues
): SyndicateUpdateRequest {
  return {
    name: values.name,
    ...(values.contact_email && { contact_email: values.contact_email }),
    ...(values.contact_phone && { contact_phone: values.contact_phone }),
  }
}
