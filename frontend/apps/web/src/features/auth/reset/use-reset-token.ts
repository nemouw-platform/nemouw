"use client"

import * as React from "react"

/**
 * The token from the email link, `/reset-password#token=…`. It lives in the
 * hash so it is never sent to a server (no logs, no Referer). `undefined`
 * until the browser has read it (server render and hydration), so both
 * renders match; `null` when the link has no token.
 */
export function useResetToken(): string | null | undefined {
  return React.useSyncExternalStore(subscribe, readToken, () => undefined)
}

function subscribe(onChange: () => void) {
  window.addEventListener("hashchange", onChange)
  return () => window.removeEventListener("hashchange", onChange)
}

function readToken(): string | null {
  return new URLSearchParams(window.location.hash.slice(1)).get("token")
}

/** Drop the spent token from the address bar and history */
export function forgetResetToken() {
  const { pathname, search } = window.location
  window.history.replaceState(window.history.state, "", pathname + search)
}
