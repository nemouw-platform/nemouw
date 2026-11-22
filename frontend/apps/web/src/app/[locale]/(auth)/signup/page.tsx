import type { Metadata } from "next"
import { useTranslations } from "next-intl"
import { getTranslations } from "next-intl/server"

import {
  AuthPrompt,
  AuthPromptLink,
} from "@/features/auth/components/auth-prompt"
import { AuthStepHeader } from "@/features/auth/components/auth-step-header"
import { SIGNUP_STEPS } from "@/features/auth/lib/constants"
import { SignupForm } from "@/features/auth/signup/signup-form"

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Signup")
  return { title: t("metaTitle"), description: t("metaDescription") }
}

export default function SignupPage() {
  const t = useTranslations("Signup")

  return (
    <div className="flex flex-col gap-8">
      <AuthStepHeader
        title={t("title")}
        description={t("description")}
        step={SIGNUP_STEPS.details}
      />
      <SignupForm />
      <AuthPrompt prompt={t("haveAccount")}>
        <AuthPromptLink href="/login">{t("logIn")}</AuthPromptLink>
      </AuthPrompt>
    </div>
  )
}
