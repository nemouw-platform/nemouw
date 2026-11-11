import {
  createApiClient,
  createAuthApi,
  createSpecialistsApi,
  createSyndicatesApi,
} from "@workspace/api"

import { endSession } from "@/features/auth/session/end-session"

// Inlined into the browser bundle at build time (see .env.example)
const baseUrl = process.env.NEXT_PUBLIC_API_URL

if (!baseUrl) {
  throw new Error(
    "NEXT_PUBLIC_API_URL is not set. Copy apps/web/.env.example to apps/web/.env.local."
  )
}

/**
 * Browser client: sends the session cookies straight to the backend. When a
 * 401 survives the automatic refresh, the session is over: sign out cleanly
 * and go to the login page (which brings the user back afterwards).
 */
export const apiClient = createApiClient({
  baseUrl,
  onSessionExpired: () => endSession(() => authApi.logout()),
})

export const authApi = createAuthApi(apiClient)

export const specialistsApi = createSpecialistsApi(apiClient)

export const syndicatesApi = createSyndicatesApi(apiClient)
