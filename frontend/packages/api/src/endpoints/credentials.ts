import type { ApiClient } from "../client"
import type {
  CredentialFields,
  CredentialResponse,
  DocumentAccessResponse,
} from "../types"
import type { CallOptions } from "./types"

const BASE = "/specialists/me/credentials"

/**
 * The signed-in specialist's credentials (`/specialists/me/credentials`).
 * Each goes to their syndicate's review queue as `pending`. Documents are
 * JPEG, PNG, WebP or PDF up to 10 MB, and private: read them through
 * documentLink. Upload failures: 413 FILE_TOO_LARGE, 415
 * UNSUPPORTED_FILE_TYPE, 422 on `file`, 429, 503 MEDIA_BUSY.
 */
export function createCredentialsApi(client: ApiClient) {
  return {
    list: (options?: CallOptions) =>
      client.get<CredentialResponse[]>(BASE, options),

    /** 201 as `pending`. 422 on the fields (expiry before issue is INVALID_FORMAT on `expiry_date`). */
    submit: (fields: CredentialFields, document: Blob, options?: CallOptions) =>
      client.post<CredentialResponse>(
        BASE,
        credentialForm(fields, document),
        options
      ),

    /**
     * Replaces the fields while pending or rejected (a rejected one goes back
     * to pending). 409 CREDENTIAL_VERIFIED once verified.
     */
    update: (id: string, fields: CredentialFields, options?: CallOptions) =>
      client.patch<CredentialResponse>(`${BASE}/${id}`, fields, options),

    /** 204 in any status; deletes the document too */
    withdraw: (id: string, options?: CallOptions) =>
      client.delete<void>(`${BASE}/${id}`, options),

    /** Like update, for the document. 409 CREDENTIAL_VERIFIED once verified. */
    replaceDocument: (id: string, document: Blob, options?: CallOptions) => {
      const form = new FormData()
      form.append("document", document)
      return client.put<CredentialResponse>(
        `${BASE}/${id}/document`,
        form,
        options
      )
    },

    /** A link to the private document, valid for a few minutes */
    documentLink: (id: string, options?: CallOptions) =>
      client.get<DocumentAccessResponse>(`${BASE}/${id}/document`, options),
  }
}

/** The multipart body for a new credential: each field as a part, then the document */
function credentialForm(fields: CredentialFields, document: Blob): FormData {
  const form = new FormData()
  for (const [name, value] of Object.entries(fields)) {
    if (value != null && value !== "") form.append(name, String(value))
  }
  form.append("document", document)
  return form
}

export type CredentialsApi = ReturnType<typeof createCredentialsApi>
