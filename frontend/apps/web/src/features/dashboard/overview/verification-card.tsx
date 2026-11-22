"use client"

import {
  Card,
  CardAction,
  CardDescription,
  CardHeader,
} from "@workspace/ui/components/card"
import { useFormatter, useTranslations } from "next-intl"

import { CardHeading } from "@/features/dashboard/card-heading"
import { StatusBadge } from "@/features/dashboard/status-badge"
import { useSpecialist } from "@/features/specialists/me/specialist-provider"

/** Whether the syndicate has verified the profile, and what that means */
export function VerificationCard() {
  const t = useTranslations("Dashboard.verification")
  const format = useFormatter()
  const { specialist } = useSpecialist()
  const status = specialist.verification_status ?? "pending"
  // When the syndicate last decided (verified or rejected)
  const reviewedAt = specialist.reviewed_at
    ? format.dateTime(new Date(specialist.reviewed_at), { dateStyle: "long" })
    : null
  const reason = status === "rejected" ? specialist.rejection_reason : null

  return (
    <Card className="[--card-spacing:--spacing(6)]">
      <CardHeader>
        <CardHeading>{t("title")}</CardHeading>
        <CardDescription className="text-pretty">
          {reason
            ? t("rejected.descriptionWithReason", { reason })
            : t(`${status}.description`, { date: reviewedAt ?? "" })}
        </CardDescription>
        <CardAction>
          <StatusBadge status={status}>{t(`${status}.label`)}</StatusBadge>
        </CardAction>
      </CardHeader>
    </Card>
  )
}
