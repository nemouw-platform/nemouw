"use client"

import { isApiError, type CredentialResponse } from "@workspace/api"
import { useTranslations } from "next-intl"
import type { UseFormReturn } from "react-hook-form"

import {
  DOCUMENT_TYPES,
  MB,
  uploadProblem,
  useUploadTexts,
} from "@/components/upload"
import {
  CREDENTIAL_FIELDS,
  toCredentialFields,
  toCredentialValues,
  type CredentialValues,
} from "@/features/specialists/credentials/credential-schema"
import { credentialsApi } from "@/lib/api"
import { applyFieldErrors } from "@/lib/forms/api-errors"
import { useFormRequest } from "@/lib/forms/use-form-request"

/** The backend's app.media.credential-document.max-size */
export const DOCUMENT_RULES = { accept: DOCUMENT_TYPES, maxBytes: 10 * MB }

/**
 * Saves the credential form: a new one with its document in one request; an
 * edit sends the fields if they changed, then the document if a new one was
 * picked. Either sends a rejected credential back for review.
 */
export function useSaveCredential(
  form: UseFormReturn<CredentialValues>,
  {
    credential,
    onSaved,
  }: {
    credential?: CredentialResponse
    onSaved: (credential: CredentialResponse) => void
  }
) {
  const t = useTranslations("Credentials.errors")
  const tFormErrors = useTranslations("Form.errors")
  const texts = useUploadTexts(DOCUMENT_RULES)

  const save = useFormRequest(
    async (signal, values: CredentialValues) => {
      const fields = toCredentialFields(values)
      if (!credential?.id) {
        return credentialsApi.submit(fields, values.document!, { signal })
      }
      const saved = toCredentialFields(toCredentialValues(credential))
      let result = credential
      if (JSON.stringify(fields) !== JSON.stringify(saved)) {
        result = await credentialsApi.update(credential.id, fields, { signal })
      }
      if (values.document) {
        result = await credentialsApi.replaceDocument(
          credential.id,
          values.document,
          { signal }
        )
      }
      return result
    },
    {
      onSuccess: onSaved,
      onError: (error) => {
        const problem = uploadProblem(error)
        if (problem) {
          form.setError(
            "document",
            { message: texts.problem(problem) },
            { shouldFocus: true }
          )
          return true
        }
        if (isApiError(error) && error.code === "CREDENTIAL_VERIFIED") {
          save.setFormError(t("verified"))
          return true
        }
        return applyFieldErrors(form, error, CREDENTIAL_FIELDS, tFormErrors)
      },
    }
  )

  return {
    submit: form.handleSubmit((values) => save.run(values)),
    isPending: save.isPending,
    formError: save.formError,
  }
}
