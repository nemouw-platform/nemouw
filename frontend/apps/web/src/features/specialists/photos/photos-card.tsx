"use client"

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
} from "@workspace/ui/components/card"
import { useTranslations } from "next-intl"

import { ImageUpload, MB } from "@/components/upload"
import { CardHeading } from "@/features/dashboard/card-heading"
import { useSpecialist } from "@/features/specialists/me/specialist-provider"
import { specialistsApi } from "@/lib/api"

// The backend's limits (app.media.*.max-size)
const PROFILE_PICTURE_MAX_BYTES = 2 * MB
const COVER_IMAGE_MAX_BYTES = 5 * MB

/** The profile picture and cover image; each saves as soon as it is picked */
export function PhotosCard() {
  const t = useTranslations("Profile.photos")
  const { specialist, updateSpecialist } = useSpecialist()

  return (
    <Card
      id="photos"
      // Clear of the sticky top bar when opened from a #photos link
      className="scroll-mt-24 [--card-spacing:--spacing(6)]"
    >
      <CardHeader>
        <CardHeading>{t("title")}</CardHeading>
        <CardDescription>{t("description")}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-6">
        <PhotoField label={t("picture")}>
          <ImageUpload
            shape="avatar"
            src={specialist.profile_picture_url}
            alt={t("picture")}
            maxBytes={PROFILE_PICTURE_MAX_BYTES}
            onUpload={async (file) => {
              const image = await specialistsApi.setProfilePicture(file)
              updateSpecialist({ profile_picture_url: image.url ?? null })
            }}
            onRemove={async () => {
              await specialistsApi.removeProfilePicture()
              updateSpecialist({ profile_picture_url: null })
            }}
          />
        </PhotoField>
        <PhotoField label={t("cover")} hint={t("coverHint")}>
          <ImageUpload
            shape="cover"
            src={specialist.cover_image_url}
            alt={t("cover")}
            maxBytes={COVER_IMAGE_MAX_BYTES}
            onUpload={async (file) => {
              const image = await specialistsApi.setCoverImage(file)
              updateSpecialist({ cover_image_url: image.url ?? null })
            }}
            onRemove={async () => {
              await specialistsApi.removeCoverImage()
              updateSpecialist({ cover_image_url: null })
            }}
          />
        </PhotoField>
      </CardContent>
    </Card>
  )
}

/** A photo's name (and what it is for) above it */
function PhotoField({
  label,
  hint,
  children,
}: {
  label: string
  hint?: string
  children: React.ReactNode
}) {
  return (
    <section className="flex flex-col gap-3">
      <div className="flex flex-col gap-0.5">
        <h3 className="text-sm font-medium">{label}</h3>
        {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
      </div>
      {children}
    </section>
  )
}
