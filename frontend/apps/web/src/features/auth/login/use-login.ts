"use client"

import { useTranslations } from "next-intl"
import * as React from "react"
import type { UseFormReturn } from "react-hook-form"

import { fieldErrorMessage } from "@/features/auth/lib/api-errors"
import { useAuthRequest } from "@/features/auth/lib/use-auth-request"
import { loginDestination } from "@/features/auth/login/login-destination"
import {
  classifyLoginError,
  type LoginFailure,
} from "@/features/auth/login/login-errors"
import type { LoginValues } from "@/features/auth/login/login-schema"
import { useRouter } from "@/i18n/navigation"
import { authApi } from "@/lib/api"

/** Failures the form shows with their own message and link */
export type LoginNotice = Extract<
  LoginFailure["kind"],
  "invalidCredentials" | "suspended" | "notVerified"
>

/**
 * Logs in (the backend sets the session cookies), then works out where the
 * user belongs, all while the button shows its pending state.
 */
export function useLogin(form: UseFormReturn<LoginValues>) {
  const router = useRouter()
  const tAuth = useTranslations("Auth.errors")
  const [notice, setNotice] = React.useState<LoginNotice | null>(null)

  const login = useAuthRequest(
    async (signal, values: LoginValues) => {
      const session = await authApi.login(values, { signal })
      return loginDestination(session.role, signal)
    },
    {
      // replace: the back button must not return to the login form
      onSuccess: (destination) => router.replace(destination),
      onError: (error) => {
        const failure = classifyLoginError(error)
        switch (failure.kind) {
          case "fields":
            failure.fields.forEach(({ name, code }) =>
              form.setError(name, { message: fieldErrorMessage(code, tAuth) })
            )
            return true
          case "general":
            return false
          default:
            setNotice(failure.kind)
            // Never echo a rejected password back into the field
            form.resetField("password")
            return true
        }
      },
    }
  )

  return {
    submit: form.handleSubmit((values) => {
      setNotice(null)
      login.run(values)
    }),
    isPending: login.isPending,
    formError: login.formError,
    notice,
  }
}
