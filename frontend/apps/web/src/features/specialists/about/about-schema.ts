import type {
  SpecialistProfileResponse,
  SpecialistProfileWriteRequest,
} from "@workspace/api"
import { z } from "zod"

import type { FormErrorsTranslator } from "@/lib/forms/api-errors"
import { createFieldRules } from "@/lib/forms/validation"

/** Named like the API body, so server field errors map 1:1 */
export const ABOUT_FIELDS = ["title", "headline", "bio"] as const

/** The backend's limits (NAME_MAX_LENGTH, HEADLINE_MAX, BIO_MAX) */
export const ABOUT_MAX_LENGTH = {
  title: 100,
  headline: 150,
  bio: 2000,
} as const

export function createAboutSchema(t: FormErrorsTranslator) {
  const rules = createFieldRules(t)
  return z.object({
    title: rules.optionalText(ABOUT_MAX_LENGTH.title),
    headline: rules.optionalText(ABOUT_MAX_LENGTH.headline),
    bio: rules.optionalText(ABOUT_MAX_LENGTH.bio),
  })
}

/** One language's text as typed, "" for an empty field */
export type AboutValues = z.infer<ReturnType<typeof createAboutSchema>>

/** A language's saved text as form values (all empty when there is none) */
export function toAboutValues(
  content: SpecialistProfileResponse | undefined
): AboutValues {
  return {
    title: content?.title ?? "",
    headline: content?.headline ?? "",
    bio: content?.bio ?? "",
  }
}

/** The PUT body: a full replacement, where a field left out is stored as null */
export function toWriteRequest(
  values: AboutValues
): SpecialistProfileWriteRequest {
  const filled = ABOUT_FIELDS.filter((field) => values[field] !== "")
  return Object.fromEntries(filled.map((field) => [field, values[field]]))
}

/** True when this language has the text patients read most: the bio */
export function hasBio(content: SpecialistProfileResponse | undefined) {
  return Boolean(content?.bio?.trim())
}
