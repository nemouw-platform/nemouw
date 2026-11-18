"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import type { CredentialResponse } from "@workspace/api"
import { Button } from "@workspace/ui/components/button"
import { useFormatter, useTranslations } from "next-intl"
import * as React from "react"
import { useForm } from "react-hook-form"

import { FormControllerField } from "@/components/form/form-controller-field"
import { FormTextField } from "@/components/form/form-field"
import { SelectControl } from "@/components/form/select-control"
import { SubmitButton } from "@/components/form/submit-button"
import { FileUpload, type StoredFile } from "@/components/upload"
import { FormCard } from "@/features/dashboard/form-card"
import {
  CREDENTIAL_TYPES,
  createCredentialSchema,
  toCredentialValues,
  type CredentialValues,
} from "@/features/specialists/credentials/credential-schema"
import { useDocumentLink } from "@/features/specialists/credentials/use-credentials"
import {
  DOCUMENT_RULES,
  useSaveCredential,
} from "@/features/specialists/credentials/use-save-credential"

interface CredentialFormProps {
  /** The credential to edit; none to add one */
  credential?: CredentialResponse
  onSaved: (credential: CredentialResponse) => void
  onCancel: () => void
}

/** Adds a credential with its document, or edits one under review */
export function CredentialForm({
  credential,
  onSaved,
  onCancel,
}: CredentialFormProps) {
  const t = useTranslations("Credentials.form")
  const tTypes = useTranslations("Credentials.types")
  const tFormErrors = useTranslations("Form.errors")
  const schema = React.useMemo(
    () =>
      createCredentialSchema(
        {
          type: t("type"),
          title: t("title"),
          organization: t("organization"),
          document: t("document"),
        },
        tFormErrors,
        { dateOrder: t("dateOrder") },
        { requireDocument: !credential }
      ),
    [t, tFormErrors, credential]
  )
  const form = useForm<CredentialValues>({
    resolver: zodResolver(schema),
    defaultValues: toCredentialValues(credential),
    mode: "onTouched",
  })
  const { submit, isPending, formError } = useSaveCredential(form, {
    credential,
    onSaved,
  })
  const stored = useStoredDocument(credential)
  const { errors } = form.formState

  const types = CREDENTIAL_TYPES.map((value) => ({
    value,
    label: tTypes(value),
  }))
  // Unique per form, as an edit form can sit beside a new one
  const id = (name: string) => `${name}-${credential?.id ?? "new"}`

  return (
    <FormCard
      title={credential ? t("editTitle") : t("newTitle")}
      description={
        credential?.verification_status === "rejected"
          ? t("resubmitDescription")
          : t("description")
      }
      onSubmit={submit}
      formError={formError}
      submit={
        <>
          <Button type="button" variant="ghost" onClick={onCancel}>
            {t("cancel")}
          </Button>
          <SubmitButton
            pending={isPending}
            pendingLabel={t("saving")}
            size="default"
          >
            {credential ? t("save") : t("submit")}
          </SubmitButton>
        </>
      }
    >
      <div className="grid gap-5 sm:grid-cols-2">
        <FormControllerField
          name="credential_type"
          id={id("credential_type")}
          control={form.control}
          required
          label={t("type")}
          error={errors.credential_type}
          render={(props) => (
            <SelectControl
              {...props}
              options={types}
              placeholder={t("typePlaceholder")}
            />
          )}
        />
        <FormTextField
          id={id("title")}
          required
          label={t("title")}
          placeholder={t("titlePlaceholder")}
          error={errors.title}
          {...form.register("title")}
        />
        <FormTextField
          id={id("issuing_organization")}
          required
          label={t("organization")}
          placeholder={t("organizationPlaceholder")}
          error={errors.issuing_organization}
          {...form.register("issuing_organization")}
        />
        <FormTextField
          id={id("credential_number")}
          label={t("number")}
          placeholder={t("numberPlaceholder")}
          dir="ltr"
          error={errors.credential_number}
          {...form.register("credential_number")}
        />
        <FormTextField
          id={id("issue_date")}
          type="date"
          label={t("issueDate")}
          placeholder={t("datePlaceholder")}
          error={errors.issue_date}
          {...form.register("issue_date")}
        />
        <FormTextField
          id={id("expiry_date")}
          type="date"
          label={t("expiryDate")}
          placeholder={t("datePlaceholder")}
          description={t("expiryHint")}
          error={errors.expiry_date}
          {...form.register("expiry_date")}
        />
      </div>
      <FormControllerField
        name="document"
        id={id("document")}
        control={form.control}
        required={!credential}
        label={t("document")}
        description={t("documentHint")}
        error={errors.document}
        render={(props) => (
          <FileUpload
            {...props}
            {...DOCUMENT_RULES}
            current={stored}
            disabled={isPending}
          />
        )}
      />
    </FormCard>
  )
}

/** The saved document of the credential being edited, with a View action */
function useStoredDocument(credential?: CredentialResponse): StoredFile | null {
  const t = useTranslations("Credentials.document")
  const format = useFormatter()
  const link = useDocumentLink(credential?.id)
  const document = credential?.document
  if (!document?.mime_type) return null

  return {
    mimeType: document.mime_type,
    label: document.mime_type === "application/pdf" ? t("pdf") : t("image"),
    detail: document.uploaded_at
      ? t("uploaded", {
          date: format.dateTime(new Date(document.uploaded_at), {
            dateStyle: "medium",
          }),
        })
      : undefined,
    onView: link.open,
    viewing: link.opening,
  }
}
