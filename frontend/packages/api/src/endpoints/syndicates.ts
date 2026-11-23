import type { ApiClient } from "../client"
import type {
  CredentialResponse,
  CredentialStatus,
  SyndicateResponse,
  SyndicateStaffInviteRequest,
  SyndicateStaffResponse,
  SyndicateSummary,
  SyndicateUpdateRequest,
} from "../types"
import type { CallOptions } from "./types"

const staffPath = (syndicateId: string, staffId?: string) =>
  `/syndicates/${syndicateId}/staff${staffId ? `/${staffId}` : ""}`

/**
 * Professional syndicates: the public directory, and what their staff see
 * and manage. Staff endpoints answer 403 to anyone not on that syndicate's
 * staff (admins excepted); owner-only ones to staff who are not the owner.
 */
export function createSyndicatesApi(client: ApiClient) {
  return {
    /** Public. The list specialists pick from during onboarding. */
    list: (options?: CallOptions) =>
      client.get<SyndicateSummary[]>("/syndicates", options),

    /** Contact details and owner. Staff of this syndicate and admins */
    get: (id: string, options?: CallOptions) =>
      client.get<SyndicateResponse>(`/syndicates/${id}`, options),

    /** Partial update of the name and contact details. Owner and admins */
    update: (id: string, body: SyndicateUpdateRequest, options?: CallOptions) =>
      client.patch<SyndicateResponse>(`/syndicates/${id}`, body, options),

    /** Staff of this syndicate and admins */
    listStaff: (syndicateId: string, options?: CallOptions) =>
      client.get<SyndicateStaffResponse[]>(staffPath(syndicateId), options),

    /**
     * Creates a pending account and emails it a link to set a password
     * (valid 24 hours). Owner and admins. 409 EMAIL_TAKEN.
     */
    inviteStaff: (
      syndicateId: string,
      body: SyndicateStaffInviteRequest,
      options?: CallOptions
    ) =>
      client.post<SyndicateStaffResponse>(
        staffPath(syndicateId),
        body,
        options
      ),

    /** Changes a staff member's title. Owner and admins */
    updateStaff: (
      syndicateId: string,
      staffId: string,
      title: string,
      options?: CallOptions
    ) =>
      client.patch<SyndicateStaffResponse>(
        staffPath(syndicateId, staffId),
        { title },
        options
      ),

    /**
     * Corrects the email of an invite not yet accepted, and sends it again.
     * 409 ACCOUNT_ALREADY_ACTIVE once accepted.
     */
    changeStaffEmail: (
      syndicateId: string,
      staffId: string,
      email: string,
      options?: CallOptions
    ) =>
      client.put<SyndicateStaffResponse>(
        `${staffPath(syndicateId, staffId)}/email`,
        { email },
        options
      ),

    /** Removes a staff member. 409 OWNER_CANNOT_BE_REMOVED for the owner */
    removeStaff: (
      syndicateId: string,
      staffId: string,
      options?: CallOptions
    ) => client.delete<void>(staffPath(syndicateId, staffId), options),

    /** The syndicate's credentials, optionally of one status. Its staff and admins */
    credentialQueue: (
      syndicateId: string,
      status?: CredentialStatus,
      options?: CallOptions
    ) =>
      client.get<CredentialResponse[]>(
        `/syndicates/${syndicateId}/credentials${status ? `?verification_status=${status}` : ""}`,
        options
      ),
  }
}

export type SyndicatesApi = ReturnType<typeof createSyndicatesApi>
