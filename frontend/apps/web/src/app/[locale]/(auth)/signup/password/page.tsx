import type { Metadata } from "next"
import { useTranslations } from "next-intl"
import { getTranslations } from "next-intl/server"
import { Suspense } from "react"

import { AuthStepHeader } from "@/features/auth/components/auth-step-header"
import { SIGNUP_STEPS } from "@/features/auth/lib/constants"
import { PasswordStep } from "@/features/auth/password/password-step"

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Password")
  return { title: t("metaTitle") }
}

export default function PasswordPage() {
  const t = useTranslations("Password")

  // The optional ?email= is read in the browser; until then the prerendered
  // page shows the title alone
  return (
    <Suspense
      fallback={
        <AuthStepHeader title={t("title")} step={SIGNUP_STEPS.password} />
      }
    >
      <PasswordStep />
    </Suspense>
  )
}
