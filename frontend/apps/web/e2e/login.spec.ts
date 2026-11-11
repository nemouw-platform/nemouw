import { expect, test } from "@playwright/test"

import {
  logIn,
  PASSWORD,
  signUp,
  uniqueEmail,
  passwordField,
} from "./support/auth"
import {
  cookieNames,
  expireAccessCookie,
  plantRevokedSession,
} from "./support/session"

test.describe("login", () => {
  test("a wrong password gets one neutral message", async ({ page }) => {
    const email = uniqueEmail("login")
    await signUp(page, email)
    await page.context().clearCookies()

    await logIn(page, email, "not-the-password-1")
    await expect(page.getByText("Invalid email or password.")).toBeVisible()
    await expect(
      page.getByRole("link", { name: "Reset your password" })
    ).toBeVisible()
    await expect(passwordField(page)).toHaveValue("")

    await logIn(page, "nobody-at-all@example.com", "whatever-password-1")
    await expect(page.getByText("Invalid email or password.")).toBeVisible()
  })

  test("logging in returns to the page that asked for it", async ({ page }) => {
    const email = uniqueEmail("next")
    await signUp(page, email)
    await page.context().clearCookies()

    await page.goto("/en/onboarding")
    await expect(page).toHaveURL(/\/en\/login\?next=%2Fonboarding$/)
    await logIn(page, email)
    await expect(page).toHaveURL(/\/en\/onboarding$/)
  })

  test("returning users are logged back in by the refresh cookie", async ({
    page,
    context,
  }) => {
    await signUp(page, uniqueEmail("resume"))
    await expireAccessCookie(context)
    expect(await cookieNames(context)).toContain("refresh_token")

    await page.goto("/en/onboarding")
    await expect(page).toHaveURL(/\/en\/onboarding$/)
    expect(await cookieNames(context)).toContain("access_token")
  })

  test("a session that dies mid-use ends on the login page", async ({
    page,
    context,
  }) => {
    await plantRevokedSession(context)

    await page.goto("/en/onboarding")
    await expect(page).toHaveURL(/\/en\/login\?next=%2Fonboarding$/)
    await expect(
      page.getByRole("heading", { name: "Log in to Nemouw" })
    ).toBeVisible()
  })

  test("password managers get the right hints", async ({ page }) => {
    await page.goto("/en/login")
    await expect(page.getByLabel("Email address")).toHaveAttribute(
      "autocomplete",
      "username"
    )
    await expect(passwordField(page)).toHaveAttribute(
      "autocomplete",
      "current-password"
    )
    expect(PASSWORD.length).toBeGreaterThan(8)
  })
})
