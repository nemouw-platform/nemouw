"use client"

import { Card, CardContent } from "@workspace/ui/components/card"
import { Skeleton } from "@workspace/ui/components/skeleton"
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@workspace/ui/components/tabs"
import { ChevronRightIcon, ClipboardCheckIcon } from "lucide-react"
import { useTranslations } from "next-intl"

import { LoadError } from "@/components/load-error"
import { PageHeader } from "@/components/page-header"
import { StatusBadge } from "@/features/dashboard/status-badge"
import { useSyndicate } from "@/features/syndicate/me/syndicate-provider"
import {
  matchesFilter,
  type QueueFilter,
} from "@/features/syndicate/review/review-queue"
import { SpecialistIdentity } from "@/features/syndicate/specialist/specialist-identity"
import { useReviewQueue } from "@/features/syndicate/review/use-review-queue"
import { specialistReviewPath } from "@/features/syndicate/sections"
import { Link } from "@/i18n/navigation"
import { generalErrorMessage } from "@/lib/forms/api-errors"

const FILTERS: QueueFilter[] = ["toReview", "verified", "rejected", "all"]

/** The review queue: the syndicate's specialists, those to review first */
export function QueueSection() {
  const t = useTranslations("Syndicate.queue")
  const tStatus = useTranslations("Syndicate.status")
  const tFormErrors = useTranslations("Form.errors")
  const { syndicate } = useSyndicate()
  const queue = useReviewQueue(syndicate.id ?? "")

  let body: React.ReactNode
  if (queue.failed) {
    body = (
      <LoadError
        message={generalErrorMessage(queue.error, tFormErrors)}
        onRetry={queue.retry}
      />
    )
  } else if (queue.isLoading || !queue.data) {
    body = <Skeleton className="h-64 w-full rounded-xl" />
  } else {
    const { entries, specialists } = queue.data
    body = (
      <Tabs defaultValue="toReview" className="gap-4">
        <TabsList aria-label={t("filters")}>
          {FILTERS.map((filter) => (
            <TabsTrigger key={filter} value={filter} className="px-3">
              {t(`filter.${filter}`)}
            </TabsTrigger>
          ))}
        </TabsList>
        {FILTERS.map((filter) => {
          const shown = entries.filter((entry) =>
            matchesFilter(filter, entry, specialists.get(entry.specialistId))
          )
          return (
            <TabsContent key={filter} value={filter}>
              {shown.length === 0 ? (
                <Card>
                  <CardContent className="flex flex-col items-center gap-3 py-10 text-center">
                    <ClipboardCheckIcon
                      aria-hidden
                      className="size-8 text-muted-foreground"
                    />
                    <p className="text-sm text-muted-foreground">
                      {t(`empty.${filter}`)}
                    </p>
                  </CardContent>
                </Card>
              ) : (
                <ul className="flex flex-col gap-3">
                  {shown.map((entry) => {
                    const specialist = specialists.get(entry.specialistId)
                    const status = specialist?.verification_status ?? "pending"
                    return (
                      <li key={entry.specialistId}>
                        <Link
                          href={specialistReviewPath(entry.specialistId)}
                          className="group flex items-center gap-4 rounded-xl bg-card p-4 ring-1 ring-foreground/10 transition-colors outline-none hover:bg-muted/40 focus-visible:ring-3 focus-visible:ring-ring/50"
                        >
                          <SpecialistIdentity specialist={specialist} />
                          <span className="ms-auto flex shrink-0 flex-col items-end gap-1.5">
                            <StatusBadge status={status}>
                              {tStatus(status)}
                            </StatusBadge>
                            <span className="text-xs text-muted-foreground">
                              {entry.pending > 0
                                ? t("pendingCredentials", {
                                    count: entry.pending,
                                  })
                                : t("credentialsReviewed")}
                            </span>
                          </span>
                          <ChevronRightIcon
                            aria-hidden
                            className="size-4 shrink-0 text-muted-foreground rtl:-scale-x-100"
                          />
                        </Link>
                      </li>
                    )
                  })}
                </ul>
              )}
            </TabsContent>
          )
        })}
      </Tabs>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("title")}
        description={t("description", { syndicate: syndicate.name ?? "" })}
      />
      {body}
    </div>
  )
}
