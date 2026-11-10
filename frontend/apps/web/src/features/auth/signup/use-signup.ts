"use client"

import { isAbortError, useRequest } from "@workspace/ui/hooks/use-request"
import { useLocale, useTranslations } from "next-intl"
import * as React from "react"
import type { UseFormReturn } from "react-hook-form"

import {
  fieldErrorMessage,
  generalErrorMessage,
} from "@/features/auth/lib/error-messages"
import {
  classifySignupError,
  type SignupFailure,
} from "@/features/auth/signup/signup-errors"
import type { SignupValues } from "@/features/auth/signup/signup-schema"
import { useRouter } from "@/i18n/navigation"
import { authApi } from "@/lib/api"

/** Error type the form marks on the email field when the address has an account */
export const EMAIL_TAKEN = "EMAIL_TAKEN"

/**
 * Submits step 1: creates the pending account (the backend emails the code),
 * then moves to the code step. Failures land on the matching field or in
 * `formError`.
 */
export function useSignup(form: UseFormReturn<SignupValues>) {
  const locale = useLocale()
  const router = useRouter()
  const tSignup = useTranslations("Signup.errors")
  const tAuth = useTranslations("Auth.errors")
  const [formError, setFormError] = React.useState<string | null>(null)

  const signup = useRequest((signal, values: SignupValues) =>
    authApi.signup({ ...values, role: "specialist", locale }, { signal })
  )

  const showFailure = (failure: SignupFailure) => {
    switch (failure.kind) {
      case "fields":
        failure.fields.forEach(({ name, code }) =>
          form.setError(name, { message: fieldErrorMessage(code, tAuth) })
        )
        return
      case "emailTaken":
        form.setError(
          "email",
          { type: EMAIL_TAKEN, message: tSignup("emailTaken") },
          { shouldFocus: true }
        )
        return
      case "codeAlreadySent":
        setFormError(
          tSignup("codeAlreadySent", { seconds: failure.retryAfterSeconds })
        )
        return
      case "general":
        setFormError(generalErrorMessage(failure.error, tAuth))
    }
  }

  const submit = form.handleSubmit(async (values) => {
    setFormError(null)
    try {
      const result = await signup.runAsync(values)
      router.push({
        pathname: "/signup/verify",
        query: { email: result.email ?? values.email },
      })
    } catch (error) {
      if (!isAbortError(error)) showFailure(classifySignupError(error))
    }
  })

  return { submit, isPending: signup.isPending, formError }
}
