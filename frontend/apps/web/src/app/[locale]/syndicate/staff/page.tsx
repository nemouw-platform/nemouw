import type { Metadata } from "next"
import { getTranslations } from "next-intl/server"

import { StaffSection } from "@/features/syndicate/staff/staff-section"

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Syndicate.staff")
  return { title: t("metaTitle") }
}

export default function StaffPage() {
  return <StaffSection />
}
