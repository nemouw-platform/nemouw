import { describe, expect, it } from "vitest"

import {
  redirectFor,
  routeAccess,
  safeNextPath,
} from "@/features/auth/session/route-access"
import type { CookieSession } from "@/features/auth/session/session-cookie"

const signedIn: CookieSession = { kind: "session", role: "specialist" }
const settingUp: CookieSession = { kind: "setup" }

describe("routeAccess", () => {
  it.each([
    ["/signup/password", "setupOnly"],
    ["/signup", "guestOnly"],
    ["/signup/verify", "guestOnly"],
    ["/login", "guestOnly"],
    ["/forgot-password", "guestOnly"],
    ["/onboarding", "protected"],
    ["/", "public"],
    ["/loginx", "public"], // prefix match needs a path boundary
  ])("%s is %s", (path, access) => {
    expect(routeAccess(path)).toBe(access)
  })
})

describe("redirectFor", () => {
  it("sends signed-out visitors of protected pages to login, keeping where they were", () => {
    expect(redirectFor("/onboarding", null, "?tab=2")).toBe(
      "/login?next=%2Fonboarding%3Ftab%3D2"
    )
  })

  it("lets signed-in users into protected pages", () => {
    expect(redirectFor("/onboarding", signedIn)).toBeNull()
  })

  it("sends signed-in users away from login and sign-up", () => {
    expect(redirectFor("/login", signedIn)).toBe("/")
    expect(redirectFor("/signup/verify", signedIn)).toBe("/")
  })

  it("lets signed-out visitors and mid-sign-up users use the guest pages", () => {
    expect(redirectFor("/login", null)).toBeNull()
    expect(redirectFor("/signup/verify", settingUp)).toBeNull()
  })

  it("only opens step 3 with the setup token", () => {
    expect(redirectFor("/signup/password", settingUp)).toBeNull()
    expect(redirectFor("/signup/password", null)).toBe("/signup")
    expect(redirectFor("/signup/password", signedIn)).toBe("/")
  })

  it("treats the setup token as signed out elsewhere", () => {
    expect(redirectFor("/onboarding", settingUp)).toBe(
      "/login?next=%2Fonboarding"
    )
  })

  it("never redirects public pages", () => {
    expect(redirectFor("/", null)).toBeNull()
    expect(redirectFor("/", signedIn)).toBeNull()
  })
})

describe("safeNextPath", () => {
  it.each(["/onboarding", "/onboarding?tab=2", "/"])("accepts %s", (value) => {
    expect(safeNextPath(value)).toBe(value)
  })

  it.each([
    null,
    "",
    "onboarding",
    "//evil.com",
    "/\\evil.com",
    "https://evil.com",
    "/ok\nSet-Cookie: x",
  ])("rejects %s", (value) => {
    expect(safeNextPath(value)).toBeNull()
  })
})
