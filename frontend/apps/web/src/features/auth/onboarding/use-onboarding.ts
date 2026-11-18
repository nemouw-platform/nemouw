"use client"

import { DASHBOARD_PATH } from "@/features/auth/lib/constants"
import { classifyOnboardingError } from "@/features/auth/onboarding/onboarding-errors"
import type { OnboardingValues } from "@/features/auth/onboarding/onboarding-schema"
import { useRouter } from "@/i18n/navigation"
import { specialistsApi } from "@/lib/api"
import { useTranslations } from "next-intl"
import * as React from "react"
import type { UseFormReturn } from "react-hook-form"

import { fieldErrorMessage } from "@/lib/forms/api-errors"
import { useFormRequest } from "@/lib/forms/use-form-request"

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
      .then(() => router.replace(DASHBOARD_PATH))
      // 404 SPECIALIST_NOT_FOUND is the expected "not onboarded yet": stay.
      // A dead session is handled by the API client (back to login).
      .catch(() => undefined)
    return () => controller.abort()
  }, [router])
}

/** Submits onboarding; success opens the dashboard */
export function useOnboarding(form: UseFormReturn<OnboardingValues>) {
  const router = useRouter()
  const t = useTranslations("Onboarding.errors")
  const tForm = useTranslations("Form.errors")

  const onboarding = useFormRequest(
    (signal, values: OnboardingValues) =>
      specialistsApi.onboard(values, { signal }),
    {
      onSuccess: () => router.replace(DASHBOARD_PATH),
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
            router.replace(DASHBOARD_PATH)
            return true
          case "fields":
            failure.fields.forEach(({ name, code }) =>
              form.setError(name, { message: fieldErrorMessage(code, tForm) })
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
