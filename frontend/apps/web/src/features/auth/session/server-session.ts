import { cookies } from "next/headers"
import { connection } from "next/server"

import {
  ACCESS_TOKEN_COOKIE,
  readAccessToken,
  type CookieSession,
} from "@/features/auth/session/session-cookie"

/**
 * Who is signed in, from the access cookie, in Server Components. The
 * signature is not checked (the backend does that on every API call), so use
 * it to choose what to show, never to authorize. It reads request cookies:
 * render the caller inside <Suspense> so the rest of the page prerenders.
 */
export async function getCookieSession(): Promise<CookieSession | null> {
  const store = await cookies()
  // The expiry check reads the clock, which only a real request may do (a
  // prerender or prefetch would freeze it)
  await connection()
  return readAccessToken(store.get(ACCESS_TOKEN_COOKIE)?.value)
}
