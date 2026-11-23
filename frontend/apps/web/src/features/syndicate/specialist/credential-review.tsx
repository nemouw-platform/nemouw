"use client"

import type { CredentialResponse } from "@workspace/api"
import { BadgeCheckIcon } from "lucide-react"
import { useTranslations } from "next-intl"
import * as React from "react"

import { ActionButton } from "@/components/action-button"
import { CredentialCardFrame } from "@/features/specialists/credentials/credential-card"
import { RejectButton } from "@/features/syndicate/review/reason-form"
import { reviewsApi } from "@/lib/api"

/**
 * A credential as its reviewer sees it: the document to check, then
 * Verify or Reject. A decision can be changed later, e.g. a verified
 * licence that turns out to be revoked.
 */
export function CredentialReview({
  credential,
  onChange,
}: {
  credential: CredentialResponse
  /** Gets the credential as the backend saved the decision */
  onChange: (credential: CredentialResponse) => void
}) {
  const t = useTranslations("Syndicate.credential")
  const [error, setError] = React.useState<string | null>(null)
  const id = credential.id ?? ""
  const status = credential.verification_status ?? "pending"

  return (
    <CredentialCardFrame
      credential={credential}
      getDocumentLink={() => reviewsApi.credentialDocument(id)}
      notice={
        error ??
        (status === "rejected" &&
          credential.rejection_reason &&
          t("rejectedBecause", { reason: credential.rejection_reason }))
      }
    >
      {status !== "rejected" && (
        <RejectButton
          label={t("reject")}
          title={t("rejectTitle")}
          description={t("rejectDescription", {
            title: credential.title ?? "",
          })}
          submitLabel={t("rejectSubmit")}
          onSubmit={async (reason, signal) =>
            onChange(await reviewsApi.rejectCredential(id, reason, { signal }))
          }
        />
      )}
      {status !== "verified" && (
        <ActionButton
          size="sm"
          icon={<BadgeCheckIcon data-icon="inline-start" />}
          onFailure={setError}
          onAction={async () => onChange(await reviewsApi.verifyCredential(id))}
        >
          {t("verify")}
        </ActionButton>
      )}
    </CredentialCardFrame>
  )
}
