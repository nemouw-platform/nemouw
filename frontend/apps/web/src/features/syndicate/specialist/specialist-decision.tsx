"use client"

import type { SpecialistResponse } from "@workspace/api"
import { Badge } from "@workspace/ui/components/badge"
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
} from "@workspace/ui/components/card"
import { BadgeCheckIcon, BanIcon, CircleSlashIcon } from "lucide-react"
import { useFormatter, useTranslations } from "next-intl"
import * as React from "react"

import { ActionButton } from "@/components/action-button"
import { ConfirmButton } from "@/components/confirm-button"
import { FormAlert } from "@/components/form/form-alert"
import { StatusBadge } from "@/features/dashboard/status-badge"
import { RejectButton } from "@/features/syndicate/review/reason-form"
import { SpecialistIdentity } from "@/features/syndicate/specialist/specialist-identity"
import { reviewsApi } from "@/lib/api"

/**
 * The specialist and the decision on them: verifying puts them in search,
 * rejecting keeps them out (with a reason they read), and suspending stops
 * the account from logging in at all.
 */
export function SpecialistDecision({
  specialist,
  onChange,
}: {
  specialist: SpecialistResponse
  /** Gets the profile as the backend saved the decision */
  onChange: (specialist: SpecialistResponse) => void
}) {
  const t = useTranslations("Syndicate.specialist")
  const tStatus = useTranslations("Syndicate.status")
  const format = useFormatter()
  const [error, setError] = React.useState<string | null>(null)
  const id = specialist.id ?? ""
  const status = specialist.verification_status ?? "pending"
  const suspended = specialist.user_status === "suspended"
  const date = (value: string) =>
    format.dateTime(new Date(value), { dateStyle: "medium" })

  const facts = [
    specialist.created_at && t("joined", { date: date(specialist.created_at) }),
    specialist.reviewed_at &&
      t("reviewed", { date: date(specialist.reviewed_at) }),
  ].filter(Boolean)

  return (
    <Card className="[--card-spacing:--spacing(5)]">
      <CardHeader className="flex flex-wrap items-center justify-between gap-3">
        <SpecialistIdentity specialist={specialist} size="lg" />
        <div className="flex flex-wrap gap-2">
          <StatusBadge status={status}>{tStatus(status)}</StatusBadge>
          {suspended && (
            <Badge variant="destructive">
              <BanIcon data-icon="inline-start" />
              {tStatus("suspended")}
            </Badge>
          )}
        </div>
      </CardHeader>

      <CardContent className="flex flex-col gap-3">
        {facts.length > 0 && (
          <p className="text-sm text-muted-foreground">{facts.join(" · ")}</p>
        )}
        <p className="text-sm text-pretty">{t(`explain.${status}`)}</p>
        {status === "rejected" && specialist.rejection_reason && (
          <FormAlert>
            {t("rejectedBecause", { reason: specialist.rejection_reason })}
          </FormAlert>
        )}
        {suspended && <FormAlert>{t("suspendedNotice")}</FormAlert>}
        <FormAlert>{error}</FormAlert>
      </CardContent>

      <CardFooter className="flex-wrap justify-end gap-2 py-3">
        {suspended ? (
          <ActionButton
            variant="ghost"
            size="sm"
            icon={<CircleSlashIcon data-icon="inline-start" />}
            onFailure={setError}
            onAction={async () =>
              onChange(await reviewsApi.unsuspendSpecialist(id))
            }
          >
            {t("unsuspend")}
          </ActionButton>
        ) : (
          <ConfirmButton
            title={t("suspend.title")}
            description={t("suspend.description", {
              username: specialist.username ?? "",
            })}
            confirmLabel={t("suspend.confirm")}
            cancelLabel={t("suspend.cancel")}
            onConfirm={async () =>
              onChange(await reviewsApi.suspendSpecialist(id))
            }
          >
            <BanIcon data-icon="inline-start" />
            {t("suspend.action")}
          </ConfirmButton>
        )}
        {status !== "rejected" && (
          <RejectButton
            label={t("reject")}
            title={t("rejectTitle")}
            description={t("rejectDescription", {
              username: specialist.username ?? "",
            })}
            submitLabel={t("rejectSubmit")}
            onSubmit={async (reason, signal) =>
              onChange(
                await reviewsApi.rejectSpecialist(id, reason, { signal })
              )
            }
          />
        )}
        {status !== "active" && (
          <ActionButton
            size="sm"
            icon={<BadgeCheckIcon data-icon="inline-start" />}
            onFailure={setError}
            onAction={async () =>
              onChange(await reviewsApi.verifySpecialist(id))
            }
          >
            {t("verify")}
          </ActionButton>
        )}
      </CardFooter>
    </Card>
  )
}
