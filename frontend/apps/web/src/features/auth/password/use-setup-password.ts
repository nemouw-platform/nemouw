"use client"

import { useTranslations } from "next-intl"
import * as React from "react"
import type { UseFormReturn } from "react-hook-form"

import { fieldErrorMessage } from "@/features/auth/lib/api-errors"
import { AFTER_SIGNUP_PATH } from "@/features/auth/lib/constants"
import { useAuthRequest } from "@/features/auth/lib/use-auth-request"
import { classifyPasswordError } from "@/features/auth/password/password-errors"
import type { PasswordValues } from "@/features/auth/password/password-schema"
import { useRouter } from "@/i18n/navigation"
import { authApi } from "@/lib/api"

/**
 * Submits step 3: sets the first password, which activates the account and
 * starts the session (the backend sets the session cookies). An expired setup
 * step is reported separately, since it needs a link back to step 2.
 */
export function useSetupPassword(form: UseFormReturn<PasswordValues>) {
  const router = useRouter()
  const tAuth = useTranslations("Auth.errors")
  const [setupExpired, setSetupExpired] = React.useState(false)

  const setup = useAuthRequest(
    (signal, values: PasswordValues) =>
      authApi.setupPassword(values, { signal }),
    {
      // replace: the back button must not return to a finished step
      onSuccess: () => router.replace(AFTER_SIGNUP_PATH),
      onError: (error) => {
        const failure = classifyPasswordError(error)
        switch (failure.kind) {
          case "setupExpired":
            setSetupExpired(true)
            return true
          case "fields":
            failure.fields.forEach(({ name, code }) =>
              form.setError(
                name,
                { message: fieldErrorMessage(code, tAuth) },
                { shouldFocus: true }
              )
            )
            return true
          case "general":
            return false
        }
      },
    }
  )

  return {
    submit: form.handleSubmit((values) => {
      setSetupExpired(false)
      setup.run(values)
    }),
    isPending: setup.isPending,
    formError: setup.formError,
    setupExpired,
  }
}
