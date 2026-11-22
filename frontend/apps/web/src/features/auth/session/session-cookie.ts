import type { Role } from "@workspace/api"

/**
 * The backend's access cookie (HttpOnly, Path=/, 15 min). It is the only auth
 * cookie our pages receive: the refresh cookie is scoped to /v1/auth.
 */
export const ACCESS_TOKEN_COOKIE = "access_token"

/** Claim names from the backend's TokenClaims.java */
const PASSWORD_SETUP_SCOPE = "password_setup"

export type CookieSession =
  /** A signed-in user */
  | { kind: "session"; role: Role }
  /** Sign-up step 3: the short-lived token that may only set the first password */
  | { kind: "setup" }

interface TokenPayload {
  role?: Role
  sid?: string
  scope?: string
  exp?: number
}

/**
 * Reads the access cookie's JWT payload, for redirects only. The signature is
 * NOT checked: that is the backend's job on every API call, so a forged cookie
 * can at most show a page whose data requests then fail.
 */
export function readAccessToken(
  token: string | undefined,
  now = Date.now()
): CookieSession | null {
  const payload = token ? decodePayload(token) : null
  if (!payload || !isUnexpired(payload, now)) return null
  if (payload.scope === PASSWORD_SETUP_SCOPE) return { kind: "setup" }
  if (payload.role && payload.sid)
    return { kind: "session", role: payload.role }
  return null
}

function isUnexpired({ exp }: TokenPayload, now: number): boolean {
  return typeof exp === "number" && exp * 1000 > now
}

function decodePayload(token: string): TokenPayload | null {
  const part = token.split(".")[1]
  if (!part) return null
  try {
    const base64 = part.replace(/-/g, "+").replace(/_/g, "/")
    const json = new TextDecoder().decode(
      Uint8Array.from(atob(base64), (char) => char.charCodeAt(0))
    )
    const payload: unknown = JSON.parse(json)
    return typeof payload === "object" && payload !== null
      ? (payload as TokenPayload)
      : null
  } catch {
    return null
  }
}
