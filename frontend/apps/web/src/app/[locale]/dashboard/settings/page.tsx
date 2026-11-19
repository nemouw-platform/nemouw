import type { Metadata } from "next"
import { getTranslations } from "next-intl/server"

import { SettingsSection } from "@/features/specialists/settings/settings-section"

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Settings")
  return { title: t("metaTitle") }
}

export default function SettingsPage() {
  return <SettingsSection />
}
