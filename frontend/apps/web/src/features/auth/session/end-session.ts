import { isApiError } from "@workspace/api"
import { defaultLocale, isLocale } from "@workspace/i18n"

import { NEXT_PARAM, routeAccess } from "@/features/auth/session/route-access"

/** Logout tries, counting the first; only a 429 earns another */
const CLEAR_ATTEMPTS = 3

let ending = false

/**
 * The session is over (an API call got 401 and the refresh failed): clear the
 * cookies, then do a full page load to the login page with `?next=` back here.
 *
 * Clearing first matters: a session revoked on the server (e.g. by a password
 * reset elsewhere) can leave an access cookie that still looks valid, and the
 * proxy would then bounce the login page back home.
 *
 * Browser only. Runs once, however many calls fail together.
 */
export function endSession(clearCookies: () => Promise<unknown>) {
  if (ending) return
  ending = true
  void clearWithRetry(clearCookies).finally(redirectToLogin)
}

/**
 * The failed call and its refresh were just sent, so the logout can land over
 * the backend's per-second rate limit. Wait out its Retry-After and try again.
 * Never rejects: the redirect happens whatever the outcome.
 */
async function clearWithRetry(
  clearCookies: () => Promise<unknown>,
  attempt = 1
): Promise<void> {
  try {
    await clearCookies()
  } catch (error) {
    const rateLimited = isApiError(error) && error.status === 429
    if (!rateLimited || attempt >= CLEAR_ATTEMPTS) return
    const seconds = error.retryAfter ?? 1
    await new Promise((resolve) => setTimeout(resolve, seconds * 1000))
    return clearWithRetry(clearCookies, attempt + 1)
  }
}

function redirectToLogin() {
  const [, first, ...rest] = window.location.pathname.split("/")
  const hasLocale = isLocale(first)
  const locale = hasLocale ? first : defaultLocale
  const path = hasLocale ? `/${rest.join("/")}` : window.location.pathname

  // Already on login / sign-up: stay, the form is what they need
  if (routeAccess(path) === "guestOnly") {
    ending = false
    return
  }
  const next = new URLSearchParams({
    [NEXT_PARAM]: path + window.location.search,
  })
  window.location.assign(`/${locale}/login?${next}`)
}
