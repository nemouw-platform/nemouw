"use client"

import { useSearchParams } from "next/navigation"
import { useTranslations } from "next-intl"
import * as React from "react"

import { AuthStepHeader } from "@/features/auth/components/auth-step-header"
import { SIGNUP_STEPS } from "@/features/auth/lib/constants"
import { richText } from "@/features/auth/lib/rich-text"
import { VerifyForm } from "@/features/auth/verify/verify-form"
import { useRouter } from "@/i18n/navigation"

/**
 * Step 2 for the email in `?email=`. Without one there is no code to check,
 * so the user goes back to step 1.
 */
export function VerifyStep() {
  const t = useTranslations("Verify")
  const router = useRouter()
  const email = useSearchParams().get("email")

  React.useEffect(() => {
    if (!email) router.replace("/signup")
  }, [email, router])

  if (!email) return null

  return (
    <div className="flex flex-col gap-8">
      <AuthStepHeader
        title={t("title")}
        description={t.rich("description", { email, ...richText })}
        step={SIGNUP_STEPS.verify}
      />
      <VerifyForm email={email} />
    </div>
  )
}
