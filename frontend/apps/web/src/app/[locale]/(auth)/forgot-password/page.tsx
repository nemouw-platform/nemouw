import type { Metadata } from "next"
import { getTranslations } from "next-intl/server"

import { ForgotFlow } from "@/features/auth/forgot/forgot-flow"

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("ForgotPassword")
  return { title: t("metaTitle") }
}

export default function ForgotPasswordPage() {
  return <ForgotFlow />
}
