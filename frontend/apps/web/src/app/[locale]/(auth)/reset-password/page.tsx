import type { Metadata } from "next"
import { getTranslations } from "next-intl/server"

import { ResetFlow } from "@/features/auth/reset/reset-flow"

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("ResetPassword")
  return { title: t("metaTitle") }
}

/** Opened from the email link: /reset-password#token=… (also used by invites) */
export default function ResetPasswordPage() {
  return <ResetFlow />
}
