"use client"

import { useTranslations } from "next-intl"
import * as React from "react"
import type { UseFormReturn } from "react-hook-form"

import { fieldErrorMessage } from "@/features/auth/lib/api-errors"
import { useAuthRequest } from "@/features/auth/lib/use-auth-request"
import { classifyResetError } from "@/features/auth/reset/reset-errors"
import type { ResetValues } from "@/features/auth/reset/reset-schema"
import { forgetResetToken } from "@/features/auth/reset/use-reset-token"
import { authApi } from "@/lib/api"

/**
 * Sets the new password. Success revokes every session of the account, so the
 * user logs in again with it; invited admins and staff set their first one here.
 */
export function useResetPassword(
  token: string,
  form: UseFormReturn<ResetValues>
) {
  const tAuth = useTranslations("Auth.errors")
  const [done, setDone] = React.useState(false)
  const [invalidLink, setInvalidLink] = React.useState(false)

  const reset = useAuthRequest(
    (signal, values: ResetValues) =>
      authApi.resetPassword({ token, ...values }, { signal }),
    {
      onSuccess: () => {
        forgetResetToken()
        setDone(true)
      },
      onError: (error) => {
        const failure = classifyResetError(error)
        switch (failure.kind) {
          case "invalidLink":
            setInvalidLink(true)
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
    submit: form.handleSubmit((values) => reset.run(values)),
    isPending: reset.isPending,
    formError: reset.formError,
    done,
    invalidLink,
  }
}
