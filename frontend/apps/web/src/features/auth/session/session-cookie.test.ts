import { describe, expect, it } from "vitest"

import { readAccessToken } from "@/features/auth/session/session-cookie"

const NOW = Date.UTC(2026, 10, 10, 12, 0, 0)
const inSeconds = (offset: number) => Math.floor(NOW / 1000) + offset

/** An unsigned JWT with the given payload (the proxy never checks signatures) */
function jwt(payload: object): string {
  // UTF-8 bytes, then base64url, as a real JWT is encoded
  const encode = (value: object) =>
    btoa(
      String.fromCharCode(...new TextEncoder().encode(JSON.stringify(value)))
    )
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/, "")
  return `${encode({ alg: "HS256" })}.${encode(payload)}.signature`
}

describe("readAccessToken", () => {
  it("reads a session token's role", () => {
    const token = jwt({
      sub: "u1",
      role: "specialist",
      sid: "s1",
      exp: inSeconds(600),
    })
    expect(readAccessToken(token, NOW)).toEqual({
      kind: "session",
      role: "specialist",
    })
  })

  it("recognises the password-setup token", () => {
    const token = jwt({
      sub: "u1",
      scope: "password_setup",
      exp: inSeconds(600),
    })
    expect(readAccessToken(token, NOW)).toEqual({ kind: "setup" })
  })

  it.each([
    ["no cookie", undefined],
    ["expired", jwt({ role: "specialist", sid: "s1", exp: inSeconds(-1) })],
    ["no expiry", jwt({ role: "specialist", sid: "s1" })],
    ["no session id", jwt({ role: "specialist", exp: inSeconds(600) })],
    ["not a JWT", "garbage"],
    ["broken payload", "a.%%%.c"],
  ])("returns null for %s", (_, token) => {
    expect(readAccessToken(token, NOW)).toBeNull()
  })

  it("decodes non-ASCII payloads (UTF-8)", () => {
    const token = jwt({ role: "patient", sid: "é", exp: inSeconds(60) })
    expect(readAccessToken(token, NOW)).toEqual({
      kind: "session",
      role: "patient",
    })
  })
})
