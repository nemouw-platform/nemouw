import type { Metadata } from "next"
import { useTranslations } from "next-intl"
import { getTranslations } from "next-intl/server"

import { SignupForm } from "@/features/auth/signup/signup-form"
import { Link } from "@/i18n/navigation"

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Signup")
  return { title: t("metaTitle"), description: t("metaDescription") }
}

export default function SignupPage() {
  const t = useTranslations("Signup")

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-semibold tracking-tight text-balance">
          {t("title")}
        </h1>
        <p className="text-sm text-muted-foreground">{t("description")}</p>
      </div>

      <SignupForm />

      <p className="text-center text-sm text-muted-foreground">
        {t("haveAccount")}{" "}
        <Link
          href="/login"
          className="font-medium text-foreground underline underline-offset-4"
        >
          {t("logIn")}
        </Link>
      </p>
    </div>
  )
}
