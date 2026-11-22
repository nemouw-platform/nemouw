import type { Metadata } from "next"
import { getTranslations } from "next-intl/server"

import { CredentialsSection } from "@/features/specialists/credentials/credentials-section"

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Credentials")
  return { title: t("metaTitle") }
}

export default function CredentialsPage() {
  return <CredentialsSection />
}
