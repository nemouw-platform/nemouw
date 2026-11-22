import type { Metadata } from "next"
import { useTranslations } from "next-intl"
import { getTranslations } from "next-intl/server"

import {
  AuthPrompt,
  AuthPromptLink,
} from "@/features/auth/components/auth-prompt"
import { AuthStepHeader } from "@/features/auth/components/auth-step-header"
import { LoginForm } from "@/features/auth/login/login-form"

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Login")
  return { title: t("metaTitle") }
}

export default function LoginPage() {
  const t = useTranslations("Login")

  return (
    <div className="flex flex-col gap-8">
      <AuthStepHeader title={t("title")} description={t("description")} />
      <LoginForm />
      <AuthPrompt prompt={t("noAccount")}>
        <AuthPromptLink href="/signup">{t("createAccount")}</AuthPromptLink>
      </AuthPrompt>
    </div>
  )
}
