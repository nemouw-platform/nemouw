"use client"

import { useTranslations } from "next-intl"

import { PageHeader } from "@/components/page-header"
import { SettingsForm } from "@/features/syndicate/settings/settings-form"

/** /syndicate/settings: the syndicate's own details */
export function SettingsSection() {
  const t = useTranslations("Syndicate.settings")

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title={t("title")} description={t("description")} />
      <SettingsForm />
    </div>
  )
}
