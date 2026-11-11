import type { Metadata } from "next"
import { useTranslations } from "next-intl"
import { getTranslations } from "next-intl/server"

import { AuthStepHeader } from "@/features/auth/components/auth-step-header"
import { OnboardingForm } from "@/features/auth/onboarding/onboarding-form"

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Onboarding")
  return { title: t("metaTitle") }
}

/** Where sign-up lands (AFTER_SIGNUP_PATH); the proxy lets only specialists in */
export default function OnboardingPage() {
  const t = useTranslations("Onboarding")

  return (
    <div className="flex flex-col gap-8">
      <AuthStepHeader title={t("title")} description={t("description")} />
      <OnboardingForm />
    </div>
  )
}
