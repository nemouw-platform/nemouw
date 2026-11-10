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

/** Message for a backend field error code, used when the server rejects a field */
export function fieldErrorMessage(
  code: FieldErrorCode,
  t: AuthErrorsTranslator
): string {
  return t(`field.${code}`)
}

/** Message for failures no form field explains: offline, or anything unexpected */
export function generalErrorMessage(
  error: unknown,
  t: AuthErrorsTranslator
): string {
  const isOffline =
    isApiError(error) && error.code === CLIENT_ERROR_CODES.network
  return t(isOffline ? "network" : "unexpected")
}
