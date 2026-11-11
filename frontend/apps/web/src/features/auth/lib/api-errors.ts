import {
  CLIENT_ERROR_CODES,
  isApiError,
  type FieldErrorCode,
} from "@workspace/api"
import type { useTranslations } from "next-intl"

/** Translator for the shared "Auth.errors" messages */
export type AuthErrorsTranslator = ReturnType<
  typeof useTranslations<"Auth.errors">
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
  t: AuthErrorsTranslator
): string {
  return t(`field.${code}`)
}

/** Message for failures no screen handles itself: offline, throttled, or unexpected */
export function generalErrorMessage(
  error: unknown,
  t: AuthErrorsTranslator
): string {
  if (isTooManyRequests(error)) {
    return t("tooManyRequests", { seconds: retryAfterSeconds(error) })
  }
  const isOffline =
    isApiError(error) && error.code === CLIENT_ERROR_CODES.network
  return t(isOffline ? "network" : "unexpected")
}
