import type { Metadata } from "next"
import { getTranslations } from "next-intl/server"

import { LocationsSection } from "@/features/specialists/locations/locations-section"

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Locations")
  return { title: t("metaTitle") }
}

export default function LocationsPage() {
  return <LocationsSection />
}
