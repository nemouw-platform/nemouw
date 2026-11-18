"use client"

import type { SpecialistVerificationStatus } from "@workspace/api"
import { Badge } from "@workspace/ui/components/badge"
import {
  Card,
  CardAction,
  CardDescription,
  CardHeader,
} from "@workspace/ui/components/card"
import { BadgeCheckIcon, CircleXIcon, ClockIcon } from "lucide-react"
import { useFormatter, useTranslations } from "next-intl"

import { CardHeading } from "@/features/dashboard/card-heading"
import { useSpecialist } from "@/features/specialists/me/specialist-provider"

const STATUS = {
  pending: { icon: ClockIcon, badge: "secondary" },
  active: { icon: BadgeCheckIcon, badge: "default" },
  rejected: { icon: CircleXIcon, badge: "destructive" },
} as const satisfies Record<
  SpecialistVerificationStatus,
  { icon: React.ComponentType; badge: string }
>

/** Whether the syndicate has verified the profile, and what that means */
export function VerificationCard() {
  const t = useTranslations("Dashboard.verification")
  const format = useFormatter()
  const { specialist } = useSpecialist()
  const status = specialist.verification_status ?? "pending"
  const { icon: Icon, badge } = STATUS[status]
  const verifiedAt = specialist.verified_at
    ? format.dateTime(new Date(specialist.verified_at), { dateStyle: "long" })
    : null

  return (
    <Card className="[--card-spacing:--spacing(6)]">
      <CardHeader>
        <CardHeading>{t("title")}</CardHeading>
        <CardDescription className="text-pretty">
          {t(`${status}.description`, { date: verifiedAt ?? "" })}
        </CardDescription>
        <CardAction>
          <Badge variant={badge}>
            <Icon data-icon="inline-start" />
            {t(`${status}.label`)}
          </Badge>
        </CardAction>
      </CardHeader>
    </Card>
  )
}
