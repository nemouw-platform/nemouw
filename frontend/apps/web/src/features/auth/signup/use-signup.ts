"use client"

import { useLocale, useTranslations } from "next-intl"
import type { UseFormReturn } from "react-hook-form"

import { fieldErrorMessage } from "@/features/auth/lib/api-errors"
import { useAuthRequest } from "@/features/auth/lib/use-auth-request"
import { classifySignupError } from "@/features/auth/signup/signup-errors"
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

  const signup = useAuthRequest(
    (signal, values: SignupValues) =>
      authApi.signup({ ...values, role: "specialist", locale }, { signal }),
    {
      onSuccess: ({ email }) =>
        router.push({
          pathname: "/signup/verify",
          query: { email: email ?? form.getValues("email") },
        }),
      onError: (error) => {
        const failure = classifySignupError(error)
        switch (failure.kind) {
          case "fields":
            failure.fields.forEach(({ name, code }) =>
              form.setError(name, { message: fieldErrorMessage(code, tAuth) })
            )
            return true
          case "emailTaken":
            form.setError(
              "email",
              { type: EMAIL_TAKEN, message: tSignup("emailTaken") },
              { shouldFocus: true }
            )
            return true
          case "codeAlreadySent":
            signup.setFormError(
              tSignup("codeAlreadySent", {
                seconds: failure.retryAfterSeconds,
              })
            )
            return true
          case "general":
            return false
        }
      },
    }
  )

  return {
    submit: form.handleSubmit((values) => signup.run(values)),
    isPending: signup.isPending,
    formError: signup.formError,
  }
}
