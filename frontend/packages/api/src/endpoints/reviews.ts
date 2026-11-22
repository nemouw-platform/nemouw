import type { ApiClient } from "../client"
import type {
  CredentialResponse,
  DocumentAccessResponse,
  SpecialistResponse,
} from "../types"
import type { CallOptions } from "./types"

/**
 * What reviewers (staff of the specialist's syndicate, and admins) decide.
 * A credential and its specialist are reviewed separately: verifying every
 * credential does not verify the specialist (docs/backend/specialist/Verification.md).
 */
export function createReviewsApi(client: ApiClient) {
  const specialist = (id: string, action = "") =>
    `/specialists/by-id/${id}${action && `/${action}`}`

  return {
    verifyCredential: (id: string, options?: CallOptions) =>
      client.post<CredentialResponse>(
        `/credentials/${id}/verify`,
        undefined,
        options
      ),

    /** 422 on `rejection_reason` (required, up to 1000 characters) */
    rejectCredential: (id: string, reason: string, options?: CallOptions) =>
      client.post<CredentialResponse>(
        `/credentials/${id}/reject`,
        { rejection_reason: reason },
        options
      ),

    /** A short-lived link to the credential's private document */
    credentialDocument: (id: string, options?: CallOptions) =>
      client.get<DocumentAccessResponse>(
        `/credentials/${id}/document`,
        options
      ),

    /** The profile, also while pending, rejected or suspended */
    getSpecialist: (id: string, options?: CallOptions) =>
      client.get<SpecialistResponse>(specialist(id), options),

    /** Status active: the specialist appears in search. Safe to repeat */
    verifySpecialist: (id: string, options?: CallOptions) =>
      client.post<SpecialistResponse>(
        specialist(id, "verify"),
        undefined,
        options
      ),

    /** Status rejected, out of search; a later verify brings them back */
    rejectSpecialist: (id: string, reason: string, options?: CallOptions) =>
      client.post<SpecialistResponse>(
        specialist(id, "reject"),
        { rejection_reason: reason },
        options
      ),

    /** The account cannot log in and leaves search; its status is kept */
    suspendSpecialist: (id: string, options?: CallOptions) =>
      client.post<SpecialistResponse>(
        specialist(id, "suspend"),
        undefined,
        options
      ),

    unsuspendSpecialist: (id: string, options?: CallOptions) =>
      client.post<SpecialistResponse>(
        specialist(id, "unsuspend"),
        undefined,
        options
      ),
  }
}

export type ReviewsApi = ReturnType<typeof createReviewsApi>
