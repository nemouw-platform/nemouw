import { createApiClient, createAuthApi } from "@workspace/api"

// Inlined into the browser bundle at build time (see .env.example)
const baseUrl = process.env.NEXT_PUBLIC_API_URL

if (!baseUrl) {
  throw new Error(
    "NEXT_PUBLIC_API_URL is not set. Copy apps/web/.env.example to apps/web/.env.local."
  )
}

/** Browser client: sends the session cookies straight to the backend */
export const apiClient = createApiClient({ baseUrl })

export const authApi = createAuthApi(apiClient)
