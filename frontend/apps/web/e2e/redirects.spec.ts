import { expect, test } from "@playwright/test"

import { signUp, uniqueEmail, passwordField } from "./support/auth"

test.describe("proxy redirects", () => {
  test("signed out", async ({ page }) => {
    await page.goto("/en/onboarding")
    await expect(page).toHaveURL(/\/en\/login\?next=%2Fonboarding$/)

    // Step 3 needs the token from step 2
    await page.goto("/ar/signup/password")
    await expect(page).toHaveURL(/\/ar\/signup$/)

    // Locale first, then auth. Visiting /ar above stored NEXT_LOCALE=ar,
    // which would (rightly) win, so check the default without cookies
    await page.context().clearCookies()
    await page.goto("/onboarding")
    await expect(page).toHaveURL(/\/en\/login\?next=%2Fonboarding$/)
  })

  test("signed in: guest pages send you home", async ({ page }) => {
    await signUp(page, uniqueEmail("guest"))

    for (const path of [
      "/en/login",
      "/en/signup",
      "/en/forgot-password",
      "/en/reset-password",
    ]) {
      await page.goto(path)
      await expect(page).toHaveURL(/\/en$/)
    }
    await page.goto("/en/signup/password")
    await expect(page).toHaveURL(/\/en$/)
  })

  test("?next= only accepts paths on this site", async ({ page }) => {
    const email = uniqueEmail("open-redirect")
    await signUp(page, email)
    await page.context().clearCookies()

    await page.goto("/en/login?next=//evil.example")
    await page.getByLabel("Email address").fill(email)
    await passwordField(page).fill("violet-harbor-lantern-42")
    await page.getByRole("button", { name: "Log in" }).click()
    await expect(page).toHaveURL(/localhost:\d+\/en\/onboarding$/)
  })
})
