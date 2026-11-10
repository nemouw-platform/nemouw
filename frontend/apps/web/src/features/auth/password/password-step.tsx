"use client"

import { useSearchParams } from "next/navigation"
import { useTranslations } from "next-intl"

import { AuthStepHeader } from "@/features/auth/components/auth-step-header"
import { SIGNUP_STEPS } from "@/features/auth/lib/constants"
import { PasswordForm } from "@/features/auth/password/password-form"

/**
 * Step 3. The setup cookie from step 2 is what authorizes it; the optional
 * `?email=` only lets the form warn about passwords that contain the email.
 */
export function PasswordStep() {
  const t = useTranslations("Password")
  const email = useSearchParams().get("email") ?? undefined

  return (
    <div className="flex flex-col gap-8">
      <AuthStepHeader
        title={t("title")}
        description={t("description")}
        step={SIGNUP_STEPS.password}
      />
      <PasswordForm email={email} />
    </div>
  )
}
