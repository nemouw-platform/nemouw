import type { Role } from "@workspace/api"

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

interface Rule {
  prefix: string
  access: Access
  /** For "protected": the roles allowed in; other signed-in users go home */
  roles?: readonly Role[]
}

/** First matching prefix wins, so more specific paths come first */
const RULES: readonly Rule[] = [
  { prefix: "/signup/password", access: "setupOnly" },
  { prefix: "/signup", access: "guestOnly" },
  { prefix: "/login", access: "guestOnly" },
  { prefix: "/forgot-password", access: "guestOnly" },
  { prefix: "/reset-password", access: "guestOnly" },
  { prefix: "/onboarding", access: "protected", roles: ["specialist"] },
]

const PUBLIC: Rule = { prefix: "", access: "public" }

function ruleFor(path: string): Rule {
  return (
    RULES.find(
      ({ prefix }) => path === prefix || path.startsWith(`${prefix}/`)
    ) ?? PUBLIC
  )
}

/** The access rule for a path without its locale, e.g. "/signup/verify" */
export function routeAccess(path: string): Access {
  return ruleFor(path).access
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
  const rule = ruleFor(path)

  switch (rule.access) {
    case "protected":
      if (!signedIn) return loginPathFor(path + search)
      return !rule.roles || rule.roles.includes(session.role)
        ? null
        : AFTER_LOGIN_PATH
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
