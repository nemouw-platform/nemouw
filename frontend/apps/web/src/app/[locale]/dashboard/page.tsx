import type { Metadata } from "next"
import { getTranslations } from "next-intl/server"

import { DashboardOverview } from "@/features/dashboard/overview/dashboard-overview"

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Dashboard.overview")
  return { title: t("metaTitle") }
}

/** A specialist's home (DASHBOARD_PATH) */
export default function DashboardPage() {
  return <DashboardOverview />
}
