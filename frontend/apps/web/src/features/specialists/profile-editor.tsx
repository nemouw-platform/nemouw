import { useTranslations } from "next-intl"

import { PageHeader } from "@/components/page-header"
import { AboutSection } from "@/features/specialists/about/about-section"
import { DetailsForm } from "@/features/specialists/details/details-form"
import { PhotosCard } from "@/features/specialists/photos/photos-card"

/** The profile page: photos, practical details, then the text in each language */
export function ProfileEditor() {
  const t = useTranslations("Profile")

  return (
    <div className="flex flex-col gap-8">
      <PageHeader title={t("title")} description={t("description")} />
      <PhotosCard />
      <DetailsForm />
      <AboutSection />
    </div>
  )
}
