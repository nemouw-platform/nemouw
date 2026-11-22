import type { Metadata } from "next"
import { getTranslations } from "next-intl/server"

import { SettingsSection } from "@/features/syndicate/settings/settings-section"

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Syndicate.settings")
  return { title: t("metaTitle") }
}

export default function SyndicateSettingsPage() {
  return <SettingsSection />
}
