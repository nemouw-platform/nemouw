import type { Metadata } from "next"
import { getTranslations } from "next-intl/server"

import { QueueSection } from "@/features/syndicate/review/queue-section"

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Syndicate.queue")
  return { title: t("metaTitle") }
}

export default function ReviewQueuePage() {
  return <QueueSection />
}
