import { expect, test } from "@playwright/test"

import {
  logIn,
  PASSWORD,
  requestResetToken,
  signUp,
  uniqueEmail,
  passwordField,
} from "./support/auth"

test.describe("password reset", () => {
  test("unknown addresses get the same answer", async ({ page }) => {
    await page.goto("/en/forgot-password")
    await page.getByLabel("Email address").fill("nobody-at-all@example.com")
    await page.getByRole("button", { name: "Send reset link" }).click()
    await expect(
      page.getByRole("heading", { name: "Check your email" })
    ).toBeVisible()
  })

  test("the emailed link sets a new password, once", async ({ page }) => {
    const email = uniqueEmail("reset")
    const newPassword = `harbor-violet-${Date.now() % 1e6}`
    await signUp(page, email)
    await page.context().clearCookies()

    const token = await requestResetToken(page, email)
    await expect(
      page.getByRole("button", { name: /^Send it again in 0:\d\d$/ })
    ).toBeDisabled()

    // The email links without a locale; the token rides in the hash
    await page.goto(`/reset-password#token=${encodeURIComponent(token)}`)
    await expect(page).toHaveURL(/\/en\/reset-password#token=/)
    await passwordField(page).fill(newPassword)
    await page.getByRole("button", { name: "Save new password" }).click()
    await expect(page.getByText("Password changed.")).toBeVisible()
    expect(new URL(page.url()).hash).toBe("")

    await logIn(page, email, PASSWORD)
    await expect(page.getByText("Invalid email or password.")).toBeVisible()
    await logIn(page, email, newPassword)
    await expect(page).toHaveURL(/\/en\/onboarding$/)

    // The same link again
    await page.context().clearCookies()
    await page.goto(`/en/reset-password#token=${encodeURIComponent(token)}`)
    await passwordField(page).fill("another-harbor-violet-1")
    await page.getByRole("button", { name: "Save new password" }).click()
    await expect(
      page.getByText("This link has expired or was already used.")
    ).toBeVisible()
  })

  test("a link without a token asks for a new one", async ({ page }) => {
    await page.goto("/en/reset-password")
    await expect(
      page.getByRole("link", { name: "Request a new link" })
    ).toBeVisible()
  })
})
