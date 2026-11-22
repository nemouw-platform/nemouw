import type { Metadata } from "next"
import { getTranslations } from "next-intl/server"

import { ProfileEditor } from "@/features/specialists/profile-editor"

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Profile")
  return { title: t("metaTitle") }
}

export default function ProfilePage() {
  return <ProfileEditor />
}
