import { AFTER_LOGIN_PATH } from "@/features/auth/lib/constants"
import type { CookieSession } from "@/features/auth/session/session-cookie"

/** Query parameter that carries the page to return to after logging in */
export const NEXT_PARAM = "next"

const LOGIN_PATH = "/login"
const SIGNUP_PATH = "/signup"

type Access =
  /** Needs a signed-in user */
  | "protected"
  /** Only for signed-out visitors (login, sign-up) */
  | "guestOnly"
  /** Sign-up step 3: needs the password-setup token */
  | "setupOnly"
  | "public"

/** First matching prefix wins, so more specific paths come first */
const RULES: ReadonlyArray<readonly [prefix: string, access: Access]> = [
  ["/signup/password", "setupOnly"],
  ["/signup", "guestOnly"],
  ["/login", "guestOnly"],
  ["/forgot-password", "guestOnly"],
  ["/onboarding", "protected"],
]

/** The access rule for a path without its locale, e.g. "/signup/verify" */
export function routeAccess(path: string): Access {
  const rule = RULES.find(
    ([prefix]) => path === prefix || path.startsWith(`${prefix}/`)
  )
  return rule?.[1] ?? "public"
}

/**
 * Where to send this visitor instead, as a path without locale (plus query),
 * or null to let the request through. `path` has no locale and no query.
 */
export function redirectFor(
  path: string,
  session: CookieSession | null,
  search = ""
): string | null {
  const signedIn = session?.kind === "session"

  switch (routeAccess(path)) {
    case "protected":
      return signedIn ? null : loginPathFor(path + search)
    case "guestOnly":
      return signedIn ? AFTER_LOGIN_PATH : null
    case "setupOnly":
      if (session?.kind === "setup") return null
      return signedIn ? AFTER_LOGIN_PATH : SIGNUP_PATH
    case "public":
      return null
  }
}

function loginPathFor(next: string): string {
  return `${LOGIN_PATH}?${new URLSearchParams({ [NEXT_PARAM]: next })}`
}

/**
 * A `next` value that is safe to navigate to: a path on this site only
 * ("/onboarding"), never "//evil.com" or "https://…" (open redirect).
 */
export function safeNextPath(value: string | null | undefined): string | null {
  if (!value || !value.startsWith("/") || value.startsWith("//")) return null
  if (value.includes("\\") || /[\u0000-\u001f]/.test(value)) return null
  return value
}
