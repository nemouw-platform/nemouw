import type {
  CredentialFields,
  CredentialResponse,
  CredentialType,
} from "@workspace/api"
import { z } from "zod"

import type { FormErrorsTranslator } from "@/lib/forms/api-errors"
import { createFieldRules } from "@/lib/forms/validation"

export const CREDENTIAL_TYPES = [
  "degree",
  "license",
  "certification",
  "membership",
] as const satisfies readonly CredentialType[]

/** Named like the API fields, so server field errors map 1:1 */
export const CREDENTIAL_FIELDS = [
  "credential_type",
  "title",
  "issuing_organization",
  "credential_number",
  "issue_date",
  "expiry_date",
  "document",
] as const

/** The backend's ValidationLimits.TEXT_MAX */
const TEXT_MAX_LENGTH = 100

interface CredentialLabels {
  type: string
  title: string
  organization: string
  document: string
}

interface CredentialMessages {
  /** The expiry date is before the issue date */
  dateOrder: string
}

/**
 * The credential form. A new credential needs its document; an edit keeps
 * the saved one unless a new file is picked.
 */
export function createCredentialSchema(
  labels: CredentialLabels,
  t: FormErrorsTranslator,
  messages: CredentialMessages,
  { requireDocument }: { requireDocument: boolean }
) {
  const rules = createFieldRules(t)
  return z
    .object({
      credential_type: rules.choice(labels.type),
      title: rules.text(labels.title, TEXT_MAX_LENGTH),
      issuing_organization: rules.text(labels.organization, TEXT_MAX_LENGTH),
      credential_number: rules.optionalText(TEXT_MAX_LENGTH),
      // "YYYY-MM-DD" from a date input, or "" when left empty
      issue_date: z.string(),
      expiry_date: z.string(),
      document: z
        .custom<File | null>((value) => value === null || value instanceof File)
        .refine((file) => !requireDocument || file !== null, {
          message: t("required", { field: labels.document }),
        }),
    })
    .refine(
      ({ issue_date, expiry_date }) =>
        !issue_date || !expiry_date || expiry_date >= issue_date,
      { path: ["expiry_date"], message: messages.dateOrder }
    )
}

export type CredentialValues = z.infer<
  ReturnType<typeof createCredentialSchema>
>

/** A saved credential as form values (empty for a new one) */
export function toCredentialValues(
  credential?: CredentialResponse
): CredentialValues {
  return {
    credential_type: credential?.credential_type ?? "",
    title: credential?.title ?? "",
    issuing_organization: credential?.issuing_organization ?? "",
    credential_number: credential?.credential_number ?? "",
    issue_date: credential?.issue_date ?? "",
    expiry_date: credential?.expiry_date ?? "",
    document: null,
  }
}

/** The fields to send; an empty optional field is left out (stored as null) */
export function toCredentialFields(values: CredentialValues): CredentialFields {
  return {
    credential_type: values.credential_type as CredentialType,
    title: values.title,
    issuing_organization: values.issuing_organization,
    ...(values.credential_number && {
      credential_number: values.credential_number,
    }),
    ...(values.issue_date && { issue_date: values.issue_date }),
    ...(values.expiry_date && { expiry_date: values.expiry_date }),
  }
}
