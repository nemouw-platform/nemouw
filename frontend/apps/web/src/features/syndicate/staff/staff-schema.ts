import type { SyndicateStaffInviteRequest } from "@workspace/api"
import { z } from "zod"

import type { FormErrorsTranslator } from "@/lib/forms/api-errors"
import { createFieldRules } from "@/lib/forms/validation"

/** Named like the API body, so server field errors map 1:1 */
export const STAFF_FIELDS = [
  "first_name",
  "last_name",
  "email",
  "title",
] as const

/** The backend's ValidationLimits.NAME_MAX, for names and titles */
const TEXT_MAX_LENGTH = 100

export function createInviteSchema(
  labels: Record<(typeof STAFF_FIELDS)[number], string>,
  t: FormErrorsTranslator
) {
  const rules = createFieldRules(t)
  return z.object({
    first_name: rules.text(labels.first_name, TEXT_MAX_LENGTH),
    last_name: rules.text(labels.last_name, TEXT_MAX_LENGTH),
    email: rules.email(labels.email),
    title: rules.text(labels.title, TEXT_MAX_LENGTH),
  })
}

export type InviteValues = z.infer<ReturnType<typeof createInviteSchema>>

export const EMPTY_INVITE: InviteValues = {
  first_name: "",
  last_name: "",
  email: "",
  title: "",
}

export function toInviteRequest(
  values: InviteValues
): SyndicateStaffInviteRequest {
  return { ...values }
}

/** Editing a member: only the title can change */
export function createTitleSchema(label: string, t: FormErrorsTranslator) {
  return z.object({
    title: createFieldRules(t).text(label, TEXT_MAX_LENGTH),
  })
}
