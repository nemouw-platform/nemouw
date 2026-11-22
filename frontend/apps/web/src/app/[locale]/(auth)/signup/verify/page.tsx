import type { Metadata } from "next"
import { useTranslations } from "next-intl"
import { getTranslations } from "next-intl/server"
import { Suspense } from "react"

import { AuthStepHeader } from "@/features/auth/components/auth-step-header"
import { SIGNUP_STEPS } from "@/features/auth/lib/constants"
import { VerifyStep } from "@/features/auth/verify/verify-step"

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Verify")
  return { title: t("metaTitle") }
}

export default function VerifyPage() {
  const t = useTranslations("Verify")

  // The email comes from the URL, read in the browser; until then the
  // prerendered page shows the title alone
  return (
    <Suspense
      fallback={
        <AuthStepHeader title={t("title")} step={SIGNUP_STEPS.verify} />
      }
    >
      <VerifyStep />
    </Suspense>
  )
}
