import { expect, type Page } from "@playwright/test"

import { logIn, passwordField, PASSWORD, requestResetToken } from "./auth"

/** The speech therapists' syndicate owner from e2e/fixtures/dev-seed.sql */
export const OWNER_EMAIL = "owner@syndicate.example"

/**
 * Signs the seeded owner in, on the review queue. The seed leaves the
 * account pending with no password: the first run sets one through
 * "Forgot password" (which also activates it), later runs just log in.
 */
export async function logInAsOwner(page: Page) {
  await page.context().clearCookies()
  await logIn(page, OWNER_EMAIL)
  const landed = await Promise.race([
    page.waitForURL(/\/en\/syndicate$/).then(() => true),
    page
      .getByText("Invalid email or password.")
      .waitFor()
      .then(() => false),
  ])
  if (landed) return

  const token = await requestResetToken(page, OWNER_EMAIL)
  await page.goto(`/en/reset-password#token=${encodeURIComponent(token)}`)
  await passwordField(page).fill(PASSWORD)
  await page.getByRole("button", { name: "Save new password" }).click()
  await expect(page.getByText("Password changed.")).toBeVisible()
  await logIn(page, OWNER_EMAIL)
  await expect(page).toHaveURL(/\/en\/syndicate$/)
}
