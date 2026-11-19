import type { Metadata } from "next"
import { getTranslations } from "next-intl/server"

import { ConditionsSection } from "@/features/specialists/conditions/conditions-section"

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Conditions")
  return { title: t("metaTitle") }
}

export default function ConditionsPage() {
  return <ConditionsSection />
}
