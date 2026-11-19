import { useTranslations } from "next-intl"

import { PageHeader } from "@/components/page-header"
import { MembershipForm } from "@/features/specialists/settings/membership-form"
import { UsernameForm } from "@/features/specialists/settings/username-form"

/** Account settings: the public username, and the syndicate membership */
export function SettingsSection() {
  const t = useTranslations("Settings")

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title={t("title")} description={t("description")} />
      <UsernameForm />
      <MembershipForm />
    </div>
  )
}
