import type { ApiClient } from "../client"
import type {
  ApiLocale,
  SpecialistOnboardingRequest,
  SpecialistProfileResponse,
  SpecialistProfileWriteRequest,
  SpecialistResponse,
  SpecialistUpdateRequest,
} from "../types"
import type { CallOptions } from "./types"

/**
 * The signed-in specialist's own profile (`/specialists/me/**`). Until
 * onboarding is done, everything but getMe and onboard answers
 * 403 ONBOARDING_REQUIRED.
 */
export function createSpecialistsApi(client: ApiClient) {
  return {
    /** 200; 404 SPECIALIST_NOT_FOUND until onboarding is complete. */
    getMe: (options?: CallOptions) =>
      client.get<SpecialistResponse>("/specialists/me", options),

    /**
     * Completes onboarding: 201. 409 USERNAME_TAKEN, MEMBER_NUMBER_TAKEN or
     * PROFILE_ALREADY_EXISTS; 422 on the fields (a reserved username is
     * INVALID_FORMAT on `username`).
     */
    onboard: (body: SpecialistOnboardingRequest, options?: CallOptions) =>
      client.post<SpecialistResponse>("/specialists/me", body, options),

    /**
     * Partial update: an omitted field is unchanged, an explicit null clears
     * it. 409 USERNAME_TAKEN, MEMBER_NUMBER_TAKEN or SYNDICATE_LOCKED; 422 on
     * the fields (min_age_treated above max_age_treated is INVALID_FORMAT on
     * `min_age_treated`).
     */
    update: (body: SpecialistUpdateRequest, options?: CallOptions) =>
      client.patch<SpecialistResponse>("/specialists/me", body, options),

    /** The public text in every locale that has some (one row per locale) */
    listContent: (options?: CallOptions) =>
      client.get<SpecialistProfileResponse[]>(
        "/specialists/me/profiles",
        options
      ),

    /**
     * Creates or replaces one locale's text: a field left out is stored as
     * null. 422 on the fields (title 100, headline 150, bio 2000 characters).
     */
    saveContent: (
      locale: ApiLocale,
      body: SpecialistProfileWriteRequest,
      options?: CallOptions
    ) =>
      client.put<SpecialistProfileResponse>(
        `/specialists/me/profiles/${locale}`,
        body,
        options
      ),
  }
}

export type SpecialistsApi = ReturnType<typeof createSpecialistsApi>
