"use client"

import { classifyResetError } from "@/features/auth/reset/reset-errors"
import type { ResetValues } from "@/features/auth/reset/reset-schema"
import { forgetResetToken } from "@/features/auth/reset/use-reset-token"
import { authApi } from "@/lib/api"
import { useTranslations } from "next-intl"
import * as React from "react"
import type { UseFormReturn } from "react-hook-form"

import { fieldErrorMessage } from "@/lib/forms/api-errors"
import { useFormRequest } from "@/lib/forms/use-form-request"

/**
 * Sets the new password. Success revokes every session of the account, so the
 * user logs in again with it; invited admins and staff set their first one here.
 */
export function useResetPassword(
  token: string,
  form: UseFormReturn<ResetValues>
) {
  const tForm = useTranslations("Form.errors")
  const [done, setDone] = React.useState(false)
  const [invalidLink, setInvalidLink] = React.useState(false)

  const reset = useFormRequest(
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
                { message: fieldErrorMessage(code, tForm) },
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
