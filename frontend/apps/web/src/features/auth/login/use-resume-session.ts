"use client"

import * as React from "react"

import { loginDestination } from "@/features/auth/login/login-destination"
import { useRouter } from "@/i18n/navigation"
import { authApi } from "@/lib/api"

/**
 * Auto-login for returning users. The browser drops the 15-minute access
 * cookie, so the proxy sends them to the login page, but the 90-day refresh
 * cookie (which only the API sees) may still be valid: try it once, quietly,
 * and move on if it works. No session: nothing happens, the form stays.
 */
export function useResumeSession() {
  const router = useRouter()

  React.useEffect(() => {
    const controller = new AbortController()
    authApi
      .refresh({ signal: controller.signal })
      .then((session) => loginDestination(session.role, controller.signal))
      .then((destination) => router.replace(destination))
      .catch(() => undefined) // signed out: show the login form
    return () => controller.abort()
  }, [router])
}
