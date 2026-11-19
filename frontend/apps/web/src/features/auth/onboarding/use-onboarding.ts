"use client"

import * as React from "react"
import type { UseFormReturn } from "react-hook-form"

import { DASHBOARD_PATH } from "@/features/auth/lib/constants"
import {
  ONBOARDING_FIELDS,
  type OnboardingValues,
} from "@/features/auth/onboarding/onboarding-schema"
import { useMembershipErrors } from "@/features/specialists/membership/use-membership-errors"
import { useRouter } from "@/i18n/navigation"
import { specialistsApi } from "@/lib/api"
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
  const onError = useMembershipErrors(form.setError, {
    fields: ONBOARDING_FIELDS,
    // Done already, e.g. in another tab: on to the dashboard
    onAlreadyOnboarded: () => router.replace(DASHBOARD_PATH),
  })

  const onboarding = useFormRequest(
    (signal, values: OnboardingValues) =>
      specialistsApi.onboard(values, { signal }),
    { onSuccess: () => router.replace(DASHBOARD_PATH), onError }
  )

  return {
    submit: form.handleSubmit((values) => onboarding.run(values)),
    isPending: onboarding.isPending,
    formError: onboarding.formError,
  }
}
