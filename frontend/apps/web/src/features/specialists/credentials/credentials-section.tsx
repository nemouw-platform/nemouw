"use client"

import { Button } from "@workspace/ui/components/button"
import { Card, CardContent } from "@workspace/ui/components/card"
import { Skeleton } from "@workspace/ui/components/skeleton"
import { FileBadgeIcon, PlusIcon } from "lucide-react"
import { useTranslations } from "next-intl"
import * as React from "react"

import { LoadError } from "@/components/load-error"
import { PageHeader } from "@/components/page-header"
import { CredentialCard } from "@/features/specialists/credentials/credential-card"
import { CredentialForm } from "@/features/specialists/credentials/credential-form"
import { useCredentials } from "@/features/specialists/credentials/use-credentials"
import { generalErrorMessage } from "@/lib/forms/api-errors"

/**
 * The credentials page: each degree, licence or membership with its review
 * status, and the form to add one or edit one under review (one at a time).
 */
export function CredentialsSection() {
  const t = useTranslations("Credentials")
  const tFormErrors = useTranslations("Form.errors")
  const { credentials, isLoading, failed, error, retry, saved, removed } =
    useCredentials()
  /** "new", a credential's id, or null when no form is open */
  const [editing, setEditing] = React.useState<string | null>(null)

  const close = () => setEditing(null)
  const addButton = (
    <Button onClick={() => setEditing("new")} disabled={editing === "new"}>
      <PlusIcon data-icon="inline-start" />
      {t("add")}
    </Button>
  )

  let list: React.ReactNode
  if (failed) {
    list = (
      <LoadError
        message={generalErrorMessage(error, tFormErrors)}
        onRetry={retry}
      />
    )
  } else if (isLoading) {
    list = <Skeleton className="h-36 w-full rounded-xl" />
  } else if (credentials.length === 0 && editing !== "new") {
    list = (
      <Card>
        <CardContent className="flex flex-col items-center gap-3 py-8 text-center">
          <FileBadgeIcon aria-hidden className="size-8 text-muted-foreground" />
          <div className="flex flex-col gap-1">
            <p className="font-medium">{t("empty.title")}</p>
            <p className="max-w-sm text-sm text-pretty text-muted-foreground">
              {t("empty.description")}
            </p>
          </div>
          {addButton}
        </CardContent>
      </Card>
    )
  } else {
    list = credentials.map((credential) =>
      editing === credential.id ? (
        <CredentialForm
          key={credential.id}
          credential={credential}
          onSaved={(result) => {
            saved(result)
            close()
          }}
          onCancel={close}
        />
      ) : (
        <CredentialCard
          key={credential.id}
          credential={credential}
          onEdit={() => setEditing(credential.id ?? null)}
          onWithdrawn={removed}
        />
      )
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("title")}
        description={t("description")}
        action={credentials.length > 0 && addButton}
      />
      {editing === "new" && (
        <CredentialForm
          onSaved={(result) => {
            saved(result)
            close()
          }}
          onCancel={close}
        />
      )}
      {list}
    </div>
  )
}
