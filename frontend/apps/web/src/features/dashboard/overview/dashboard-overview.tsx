"use client"

import { useTranslations } from "next-intl"

import { PageHeader } from "@/components/page-header"
import { SetupChecklist } from "@/features/dashboard/overview/setup-checklist"
import { VerificationCard } from "@/features/dashboard/overview/verification-card"
import { useSpecialist } from "@/features/specialists/me/specialist-provider"

/** The dashboard's home: verification status and what to fill in next */
export function DashboardOverview() {
  const t = useTranslations("Dashboard.overview")
  const { specialist } = useSpecialist()

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("title")}
        description={t("description", { username: specialist.username ?? "" })}
      />
      <VerificationCard />
      <SetupChecklist />
    </div>
  )
}
