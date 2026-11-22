import type { BrowserContext } from "@playwright/test"

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"
const COOKIE_HOST = new URL(API_URL).hostname

/** What the browser does when the 15-minute access cookie expires */
export async function expireAccessCookie(context: BrowserContext) {
  await context.clearCookies({ name: "access_token" })
}

/**
 * A session the server has revoked while the browser still holds a token that
 * looks valid: the proxy lets the page load, the API answers 401.
 */
export async function plantRevokedSession(context: BrowserContext) {
  const encode = (value: object) =>
    Buffer.from(JSON.stringify(value)).toString("base64url")
  const payload = {
    sub: "00000000-0000-0000-0000-000000000000",
    role: "specialist",
    sid: "revoked",
    exp: Math.floor(Date.now() / 1000) + 600,
  }
  await context.clearCookies()
  await context.addCookies([
    {
      name: "access_token",
      value: `${encode({ alg: "HS256" })}.${encode(payload)}.forged`,
      domain: COOKIE_HOST,
      path: "/",
    },
  ])
}

export async function cookieNames(context: BrowserContext) {
  return (await context.cookies()).map((cookie) => cookie.name)
}
