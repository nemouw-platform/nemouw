"use client"

import { useTranslations } from "next-intl"
import * as React from "react"
import type { UseFormReturn } from "react-hook-form"

import { fieldErrorMessage } from "@/features/auth/lib/api-errors"
import { AFTER_LOGIN_PATH } from "@/features/auth/lib/constants"
import { useAuthRequest } from "@/features/auth/lib/use-auth-request"
import { classifyOnboardingError } from "@/features/auth/onboarding/onboarding-errors"
import type { OnboardingValues } from "@/features/auth/onboarding/onboarding-schema"
import { useRouter } from "@/i18n/navigation"
import { specialistsApi } from "@/lib/api"

/**
 * Sends the specialist away if onboarding is already done (they have a
 * profile). Signed-out visitors never get here: the proxy sends them to login.
 */
export function useRedirectIfOnboarded() {
  const router = useRouter()

  React.useEffect(() => {
    const controller = new AbortController()
    specialistsApi
      .getMe({ signal: controller.signal })
      .then(() => router.replace(AFTER_LOGIN_PATH))
      // 404 SPECIALIST_NOT_FOUND is the expected "not onboarded yet": stay.
      // A dead session is handled by the API client (back to login).
      .catch(() => undefined)
    return () => controller.abort()
  }, [router])
}

/** Submits onboarding; success opens the rest of the app */
export function useOnboarding(form: UseFormReturn<OnboardingValues>) {
  const router = useRouter()
  const t = useTranslations("Onboarding.errors")
  const tAuth = useTranslations("Auth.errors")

  const onboarding = useAuthRequest(
    (signal, values: OnboardingValues) =>
      specialistsApi.onboard(values, { signal }),
    {
      onSuccess: () => router.replace(AFTER_LOGIN_PATH),
      onError: (error) => {
        const failure = classifyOnboardingError(error)
        const focus = { shouldFocus: true }
        switch (failure.kind) {
          case "usernameTaken":
            form.setError("username", { message: t("usernameTaken") }, focus)
            return true
          case "usernameUnavailable":
            form.setError(
              "username",
              { message: t("usernameUnavailable") },
              focus
            )
            return true
          case "memberNumberTaken":
            form.setError(
              "syndicate_member_number",
              { message: t("memberNumberTaken") },
              focus
            )
            return true
          case "alreadyOnboarded":
            router.replace(AFTER_LOGIN_PATH)
            return true
          case "fields":
            failure.fields.forEach(({ name, code }) =>
              form.setError(name, { message: fieldErrorMessage(code, tAuth) })
            )
            return true
          case "general":
            return false
        }
      },
    }
  )

  return {
    submit: form.handleSubmit((values) => onboarding.run(values)),
    isPending: onboarding.isPending,
    formError: onboarding.formError,
  }
}
