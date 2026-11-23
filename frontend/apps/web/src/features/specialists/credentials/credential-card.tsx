"use client"

import type { CredentialResponse, DocumentAccessResponse } from "@workspace/api"
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

import { ConfirmButton } from "@/components/confirm-button"
import { FormAlert } from "@/components/form/form-alert"
import { FileTypeIcon } from "@/components/upload"
import { CardHeading } from "@/features/dashboard/card-heading"
import { StatusBadge } from "@/features/dashboard/status-badge"
import { useDocumentLink } from "@/features/specialists/credentials/use-credentials"
import { credentialsApi } from "@/lib/api"

interface CredentialCardFrameProps {
  credential: CredentialResponse
  /** Under the header, e.g. why it was rejected */
  notice?: React.ReactNode
  /** Fetches a short-lived link to the document; adds "View document" */
  getDocumentLink?: () => Promise<DocumentAccessResponse>
  /** The actions after "View document" */
  children?: React.ReactNode
}

/**
 * One credential as a card: what it is, where its review stands, its
 * document, and the actions of whoever is looking (its specialist, or a
 * reviewer)
 */
export function CredentialCardFrame({
  credential,
  notice,
  getDocumentLink,
  children,
}: CredentialCardFrameProps) {
  const t = useTranslations("Credentials")
  const format = useFormatter()
  const link = useDocumentLink(getDocumentLink)
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

      {notice && (
        <CardContent>
          <FormAlert>{notice}</FormAlert>
        </CardContent>
      )}
      {link.failed && (
        <CardContent>
          <FormAlert>{t("card.viewFailed")}</FormAlert>
        </CardContent>
      )}

      <CardFooter className="flex-wrap justify-end gap-2 py-3">
        {credential.document && getDocumentLink && (
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
        {children}
      </CardFooter>
    </Card>
  )
}

/** A specialist's own credential: withdraw it, or edit it while under review */
export function CredentialCard({
  credential,
  onEdit,
  onWithdrawn,
}: {
  credential: CredentialResponse
  onEdit: () => void
  onWithdrawn: (id: string) => void
}) {
  const t = useTranslations("Credentials")
  const status = credential.verification_status ?? "pending"
  const id = credential.id

  return (
    <CredentialCardFrame
      credential={credential}
      getDocumentLink={id ? () => credentialsApi.documentLink(id) : undefined}
      notice={
        status === "rejected" &&
        (credential.rejection_reason
          ? t("card.rejectedBecause", { reason: credential.rejection_reason })
          : t("card.rejected"))
      }
    >
      <WithdrawButton credential={credential} onWithdrawn={onWithdrawn} />
      {/* A verified credential cannot change: withdraw it and add a new one */}
      {status !== "verified" && (
        <Button variant="outline" size="sm" onClick={onEdit}>
          <PencilIcon data-icon="inline-start" />
          {t("card.edit")}
        </Button>
      )}
    </CredentialCardFrame>
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

  return (
    <ConfirmButton
      title={t("title")}
      description={t(
        credential.verification_status === "verified"
          ? "verifiedDescription"
          : "description",
        { title: credential.title ?? "" }
      )}
      confirmLabel={t("confirm")}
      cancelLabel={t("cancel")}
      onConfirm={async () => {
        if (!credential.id) return
        await credentialsApi.withdraw(credential.id)
        onWithdrawn(credential.id)
      }}
    >
      <Trash2Icon data-icon="inline-start" />
      {t("action")}
    </ConfirmButton>
  )
}
