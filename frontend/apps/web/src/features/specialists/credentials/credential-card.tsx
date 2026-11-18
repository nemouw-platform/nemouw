"use client"

import type { CredentialResponse } from "@workspace/api"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@workspace/ui/components/alert-dialog"
import { Button } from "@workspace/ui/components/button"
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
} from "@workspace/ui/components/card"
import {
  ExternalLinkIcon,
  LoaderCircleIcon,
  PencilIcon,
  Trash2Icon,
} from "lucide-react"
import { useFormatter, useTranslations } from "next-intl"
import * as React from "react"

import { FormAlert } from "@/components/form/form-alert"
import { FileTypeIcon } from "@/components/upload"
import { CardHeading } from "@/features/dashboard/card-heading"
import { StatusBadge } from "@/features/dashboard/status-badge"
import { useDocumentLink } from "@/features/specialists/credentials/use-credentials"
import { credentialsApi } from "@/lib/api"
import { generalErrorMessage } from "@/lib/forms/api-errors"

interface CredentialCardProps {
  credential: CredentialResponse
  onEdit: () => void
  onWithdrawn: (id: string) => void
}

/** One credential: what it is, where its review stands, and its actions */
export function CredentialCard({
  credential,
  onEdit,
  onWithdrawn,
}: CredentialCardProps) {
  const t = useTranslations("Credentials")
  const format = useFormatter()
  const link = useDocumentLink(credential.id)
  const status = credential.verification_status ?? "pending"
  // Date-only values: read and shown as calendar days, whatever the time zone
  const day = (value: string) =>
    format.dateTime(new Date(value), { dateStyle: "medium", timeZone: "UTC" })

  const facts = [
    credential.credential_number &&
      t("card.number", { number: credential.credential_number }),
    credential.issue_date &&
      t("card.issued", { date: day(credential.issue_date) }),
    credential.expiry_date &&
      t("card.expires", { date: day(credential.expiry_date) }),
  ].filter(Boolean)

  return (
    <Card className="[--card-spacing:--spacing(5)]">
      <CardHeader>
        <div className="flex items-start gap-3">
          <FileTypeIcon
            mimeType={credential.document?.mime_type ?? "application/pdf"}
          />
          <div className="flex min-w-0 flex-col gap-1">
            <CardHeading>{credential.title}</CardHeading>
            <CardDescription>
              {[
                credential.credential_type &&
                  t(`types.${credential.credential_type}`),
                credential.issuing_organization,
              ]
                .filter(Boolean)
                .join(" · ")}
            </CardDescription>
            {facts.length > 0 && (
              <p className="text-xs text-muted-foreground">
                {facts.join(" · ")}
              </p>
            )}
          </div>
        </div>
        <CardAction>
          <StatusBadge status={status}>{t(`status.${status}`)}</StatusBadge>
        </CardAction>
      </CardHeader>

      {status === "rejected" && (
        <CardContent>
          <FormAlert>
            {credential.rejection_reason
              ? t("card.rejectedBecause", {
                  reason: credential.rejection_reason,
                })
              : t("card.rejected")}
          </FormAlert>
        </CardContent>
      )}
      {link.failed && (
        <CardContent>
          <FormAlert>{t("card.viewFailed")}</FormAlert>
        </CardContent>
      )}

      <CardFooter className="flex-wrap justify-end gap-2 py-3">
        {credential.document && (
          <Button
            variant="ghost"
            size="sm"
            disabled={link.opening}
            onClick={() => void link.open()}
          >
            {link.opening ? (
              <LoaderCircleIcon
                className="animate-spin"
                data-icon="inline-start"
              />
            ) : (
              <ExternalLinkIcon data-icon="inline-start" />
            )}
            {t("card.view")}
          </Button>
        )}
        <WithdrawButton credential={credential} onWithdrawn={onWithdrawn} />
        {/* A verified credential cannot change: withdraw it and add a new one */}
        {status !== "verified" && (
          <Button variant="outline" size="sm" onClick={onEdit}>
            <PencilIcon data-icon="inline-start" />
            {t("card.edit")}
          </Button>
        )}
      </CardFooter>
    </Card>
  )
}

/** Withdraw, after a confirmation that says what it removes */
function WithdrawButton({
  credential,
  onWithdrawn,
}: {
  credential: CredentialResponse
  onWithdrawn: (id: string) => void
}) {
  const t = useTranslations("Credentials.withdraw")
  const tFormErrors = useTranslations("Form.errors")
  const [open, setOpen] = React.useState(false)
  const [pending, setPending] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)

  async function withdraw() {
    if (!credential.id) return
    setPending(true)
    setError(null)
    try {
      await credentialsApi.withdraw(credential.id)
      setOpen(false)
      onWithdrawn(credential.id)
    } catch (failure) {
      setError(generalErrorMessage(failure, tFormErrors))
    } finally {
      setPending(false)
    }
  }

  return (
    <AlertDialog
      open={open}
      onOpenChange={(next) => {
        if (pending) return
        setOpen(next)
        setError(null)
      }}
    >
      <AlertDialogTrigger render={<Button variant="ghost" size="sm" />}>
        <Trash2Icon data-icon="inline-start" />
        {t("action")}
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{t("title")}</AlertDialogTitle>
          <AlertDialogDescription>
            {t(
              credential.verification_status === "verified"
                ? "verifiedDescription"
                : "description",
              { title: credential.title ?? "" }
            )}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <FormAlert>{error}</FormAlert>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={pending}>
            {t("cancel")}
          </AlertDialogCancel>
          <AlertDialogAction
            variant="destructive"
            disabled={pending}
            onClick={() => void withdraw()}
          >
            {pending && (
              <LoaderCircleIcon
                className="animate-spin"
                data-icon="inline-start"
              />
            )}
            {t("confirm")}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
