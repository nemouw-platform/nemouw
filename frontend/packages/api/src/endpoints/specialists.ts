import type { ApiClient } from "../client"
import type { SpecialistOnboardingRequest, SpecialistResponse } from "../types"
import type { CallOptions } from "./types"

/** The signed-in specialist's own profile (`/specialists/me`) */
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
  }
}

export type SpecialistsApi = ReturnType<typeof createSpecialistsApi>
