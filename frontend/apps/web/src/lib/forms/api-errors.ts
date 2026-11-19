import {
  CLIENT_ERROR_CODES,
  isApiError,
  type FieldErrorCode,
} from "@workspace/api"
import type { useTranslations } from "next-intl"
import type { FieldValues, Path, UseFormReturn } from "react-hook-form"

/** Translator for the shared "Form.errors" messages */
export type FormErrorsTranslator = ReturnType<
  typeof useTranslations<"Form.errors">
>

/** Wait used when a 429 arrives without Retry-After */
const DEFAULT_RETRY_AFTER_SECONDS = 60

/** Seconds to wait from a 429, with a sensible default */
export function retryAfterSeconds(error: unknown): number {
  return (
    (isApiError(error) ? error.retryAfter : null) ?? DEFAULT_RETRY_AFTER_SECONDS
  )
}

export function isTooManyRequests(error: unknown): boolean {
  return isApiError(error) && error.code === "TOO_MANY_REQUESTS"
}

/**
 * The 422 field errors that belong to a form's fields (others, e.g. `role`,
 * have no input to show them on). Empty for any other error.
 */
export function pickFieldErrors<TField extends string>(
  error: unknown,
  fields: readonly TField[]
): Array<{ name: TField; code: FieldErrorCode }> {
  if (!isApiError(error) || error.code !== "VALIDATION_ERROR") return []
  const isFormField = (field: string): field is TField =>
    (fields as readonly string[]).includes(field)
  return error.fieldErrors
    .filter((fieldError) => isFormField(fieldError.field))
    .map((fieldError) => ({
      name: fieldError.field as TField,
      code: fieldError.code,
    }))
}

/** Message for a backend field error code, used when the server rejects a field */
export function fieldErrorMessage(
  code: FieldErrorCode,
  t: FormErrorsTranslator
): string {
  return t(`field.${code}`)
}

/**
 * Puts a 422's field errors on the form's fields (focusing the first) and
 * returns true, or returns false when the error has none for this form.
 * `fields` are the form's field names, which match the API body's keys.
 */
export function applyFieldErrors<TValues extends FieldValues>(
  form: Pick<UseFormReturn<TValues>, "setError">,
  error: unknown,
  fields: readonly Path<TValues>[],
  t: FormErrorsTranslator
): boolean {
  const found = pickFieldErrors(error, fields)
  found.forEach(({ name, code }, index) =>
    form.setError(
      name,
      { message: fieldErrorMessage(code, t) },
      { shouldFocus: index === 0 }
    )
  )
  return found.length > 0
}

/** Message for failures no screen handles itself: offline, throttled, or unexpected */
export function generalErrorMessage(
  error: unknown,
  t: FormErrorsTranslator
): string {
  if (isTooManyRequests(error)) {
    return t("tooManyRequests", { seconds: retryAfterSeconds(error) })
  }
  const isOffline =
    isApiError(error) && error.code === CLIENT_ERROR_CODES.network
  return t(isOffline ? "network" : "unexpected")
}
