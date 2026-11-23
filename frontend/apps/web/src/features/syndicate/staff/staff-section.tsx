"use client"

import type { SyndicateStaffResponse } from "@workspace/api"
import { Button } from "@workspace/ui/components/button"
import { Skeleton } from "@workspace/ui/components/skeleton"
import { UserPlusIcon } from "lucide-react"
import { useTranslations } from "next-intl"
import * as React from "react"

import { LoadError } from "@/components/load-error"
import { PageHeader } from "@/components/page-header"
import { FormDialog } from "@/features/dashboard/form-dialog"
import { useSyndicate } from "@/features/syndicate/me/syndicate-provider"
import { StaffCard } from "@/features/syndicate/staff/staff-card"
import { InviteForm, TitleForm } from "@/features/syndicate/staff/staff-forms"
import { useStaff } from "@/features/syndicate/staff/use-staff"
import { generalErrorMessage } from "@/lib/forms/api-errors"

/** What the popup shows: the invite form, or one member's title */
type Editing =
  { kind: "invite" } | { kind: "title"; member: SyndicateStaffResponse }

/**
 * /syndicate/staff: who reviews for the syndicate. Everyone sees the list;
 * the owner invites, renames and removes.
 */
export function StaffSection() {
  const t = useTranslations("Syndicate.staff")
  const tFormErrors = useTranslations("Form.errors")
  const { syndicate, member: me, isOwner } = useSyndicate()
  const syndicateId = syndicate.id ?? ""
  const load = useStaff(syndicateId)
  const [editing, setEditing] = React.useState<Editing | null>(null)
  const close = () => setEditing(null)

  let body: React.ReactNode
  if (load.failed) {
    body = (
      <LoadError
        message={generalErrorMessage(load.error, tFormErrors)}
        onRetry={load.retry}
      />
    )
  } else if (!load.staff) {
    body = <Skeleton className="h-48 w-full rounded-xl" />
  } else {
    body = (
      <ul className="flex flex-col gap-3">
        {load.staff.map((member) => (
          <StaffCard
            key={member.id}
            member={member}
            isYou={member.id === me.id}
            canManage={isOwner}
            onEdit={() => setEditing({ kind: "title", member })}
            onRemoved={load.removed}
          />
        ))}
      </ul>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("title")}
        description={isOwner ? t("ownerDescription") : t("description")}
        action={
          isOwner && (
            <Button onClick={() => setEditing({ kind: "invite" })}>
              <UserPlusIcon data-icon="inline-start" />
              {t("invite")}
            </Button>
          )
        }
      />
      {body}
      <FormDialog open={editing !== null} onClose={close}>
        {editing?.kind === "invite" && (
          <InviteForm
            syndicateId={syndicateId}
            onSaved={(member) => {
              load.added(member)
              close()
            }}
            onCancel={close}
          />
        )}
        {editing?.kind === "title" && (
          <TitleForm
            key={editing.member.id}
            syndicateId={syndicateId}
            member={editing.member}
            onSaved={(member) => {
              load.replaced(member)
              close()
            }}
            onCancel={close}
          />
        )}
      </FormDialog>
    </div>
  )
}
