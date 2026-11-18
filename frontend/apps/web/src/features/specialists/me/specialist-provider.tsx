"use client"

import { isApiError, type SpecialistResponse } from "@workspace/api"
import { useTranslations } from "next-intl"
import * as React from "react"

import { LoadError } from "@/components/load-error"
import { AFTER_SIGNUP_PATH } from "@/features/auth/lib/constants"
import { useRouter } from "@/i18n/navigation"
import { specialistsApi } from "@/lib/api"
import { generalErrorMessage } from "@/lib/forms/api-errors"
import { useApiLoad } from "@/lib/use-api-load"

interface SpecialistContextValue {
  specialist: SpecialistResponse
  /** Swap in the profile a save returned, so every section shows it */
  setSpecialist: (specialist: SpecialistResponse) => void
}

const SpecialistContext = React.createContext<SpecialistContextValue | null>(
  null
)

/** No profile yet (404) or a profile call before onboarding (403) */
function needsOnboarding(error: unknown): boolean {
  return (
    isApiError(error) &&
    (error.code === "SPECIALIST_NOT_FOUND" ||
      error.code === "ONBOARDING_REQUIRED")
  )
}

/**
 * Loads the signed-in specialist's profile once for every page under it.
 * Shows `fallback` until it arrives, sends specialists who have not onboarded
 * yet to onboarding, and offers a retry when the load fails. A dead session
 * is the API client's job (back to login).
 */
export function SpecialistProvider({
  children,
  fallback,
}: {
  children: React.ReactNode
  fallback: React.ReactNode
}) {
  const router = useRouter()
  const t = useTranslations("Form.errors")
  const [specialist, setSpecialist] = React.useState<SpecialistResponse | null>(
    null
  )

  const { error, failed, retry } = useApiLoad(
    (signal) => specialistsApi.getMe({ signal }),
    {
      onSuccess: setSpecialist,
      onError: (loadError) => {
        if (needsOnboarding(loadError)) router.replace(AFTER_SIGNUP_PATH)
      },
    }
  )

  const value = React.useMemo(
    () => (specialist ? { specialist, setSpecialist } : null),
    [specialist]
  )

  if (value) {
    return (
      <SpecialistContext.Provider value={value}>
        {children}
      </SpecialistContext.Provider>
    )
  }
  if (failed && !needsOnboarding(error)) {
    return <LoadError message={generalErrorMessage(error, t)} onRetry={retry} />
  }
  return fallback
}

/** The signed-in specialist, inside SpecialistProvider */
export function useSpecialist(): SpecialistContextValue {
  const value = React.useContext(SpecialistContext)
  if (!value) {
    throw new Error("useSpecialist must be used inside SpecialistProvider")
  }
  return value
}
