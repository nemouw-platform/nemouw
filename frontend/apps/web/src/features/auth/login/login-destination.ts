import { isApiError, type Role } from "@workspace/api"

import {
  AFTER_LOGIN_PATH,
  AFTER_SIGNUP_PATH,
} from "@/features/auth/lib/constants"
import { NEXT_PARAM, safeNextPath } from "@/features/auth/session/route-access"
import { specialistsApi } from "@/lib/api"

/**
 * Where a user goes right after logging in (or resuming a session):
 * a specialist without a profile (404 SPECIALIST_NOT_FOUND) must onboard
 * first; anyone else returns to `?next=` (the page the proxy sent them away
 * from) or the default page.
 */
export async function loginDestination(
  role: Role,
  signal?: AbortSignal
): Promise<string> {
  if (role === "specialist" && !(await hasSpecialistProfile(signal))) {
    return AFTER_SIGNUP_PATH
  }
  return nextFromLocation() ?? AFTER_LOGIN_PATH
}

async function hasSpecialistProfile(signal?: AbortSignal): Promise<boolean> {
  try {
    await specialistsApi.getMe({ signal })
    return true
  } catch (error) {
    if (isApiError(error) && error.code === "SPECIALIST_NOT_FOUND") return false
    throw error
  }
}

/** The safe `?next=` of the current page (read at call time, browser only) */
function nextFromLocation(): string | null {
  return safeNextPath(
    new URLSearchParams(window.location.search).get(NEXT_PARAM)
  )
}
