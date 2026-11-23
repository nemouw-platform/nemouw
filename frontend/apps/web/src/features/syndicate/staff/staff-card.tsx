"use client"

import type { SyndicateStaffResponse } from "@workspace/api"
import { Badge } from "@workspace/ui/components/badge"
import { Button } from "@workspace/ui/components/button"
import { CrownIcon, MailIcon, PencilIcon, Trash2Icon } from "lucide-react"
import { useTranslations } from "next-intl"

import { ConfirmButton } from "@/components/confirm-button"
import { syndicatesApi } from "@/lib/api"

interface StaffCardProps {
  member: SyndicateStaffResponse
  /** The signed-in user's own row */
  isYou: boolean
  /** Show the owner's actions (edit the title, remove) */
  canManage: boolean
  onEdit: () => void
  onRemoved: (id: string) => void
}

/** One staff member: who they are, their title, and whether they joined yet */
export function StaffCard({
  member,
  isYou,
  canManage,
  onEdit,
  onRemoved,
}: StaffCardProps) {
  const t = useTranslations("Syndicate.staff")
  const name =
    `${member.user?.first_name ?? ""} ${member.user?.last_name ?? ""}`.trim()
  const invited = member.user?.user_status === "pending"

  return (
    <li className="flex flex-col gap-3 rounded-xl bg-card p-4 ring-1 ring-foreground/10 sm:flex-row sm:items-center">
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-medium">{name}</span>
          {isYou && (
            <span className="text-xs text-muted-foreground">{t("you")}</span>
          )}
          {member.is_owner && (
            <Badge>
              <CrownIcon data-icon="inline-start" />
              {t("owner")}
            </Badge>
          )}
          {invited && (
            <Badge variant="secondary">
              <MailIcon data-icon="inline-start" />
              {t("invited")}
            </Badge>
          )}
        </div>
        <span className="text-sm text-muted-foreground">
          {[member.title, member.user?.email].filter(Boolean).join(" · ")}
        </span>
      </div>

      {canManage && (
        <div className="flex shrink-0 justify-end gap-2">
          {/* The owner stays until an admin hands ownership over */}
          {!member.is_owner && (
            <ConfirmButton
              title={t("remove.title")}
              description={t("remove.description", { name })}
              confirmLabel={t("remove.confirm")}
              cancelLabel={t("remove.cancel")}
              onConfirm={async () => {
                if (!member.id || !member.syndicate_id) return
                await syndicatesApi.removeStaff(member.syndicate_id, member.id)
                onRemoved(member.id)
              }}
            >
              <Trash2Icon data-icon="inline-start" />
              {t("remove.action")}
            </ConfirmButton>
          )}
          <Button variant="outline" size="sm" onClick={onEdit}>
            <PencilIcon data-icon="inline-start" />
            {t("edit")}
          </Button>
        </div>
      )}
    </li>
  )
}
