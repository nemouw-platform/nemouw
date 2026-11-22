import { expect, test } from "@playwright/test"

import { signUp, uniqueEmail } from "./support/auth"
import { onboard, requireSyndicates } from "./support/onboarding"

// Desktop width: the account buttons sit in the bar (from the xl breakpoint)
test.describe("navbar account buttons", () => {
  test("signed out: log in and join", async ({ page }) => {
    await page.goto("/en")
    const bar = page.getByRole("banner")

    await expect(bar.getByRole("link", { name: "Log in" })).toBeVisible()
    await expect(
      bar.getByRole("link", { name: "Join as a specialist" })
    ).toBeVisible()
    await expect(bar.getByRole("link", { name: "Dashboard" })).toHaveCount(0)
  })

  test("a specialist gets the dashboard and log out instead", async ({
    page,
  }) => {
    await requireSyndicates(page)
    await signUp(page, uniqueEmail("navbar"))
    await onboard(page)

    await page.goto("/en")
    const bar = page.getByRole("banner")
    await expect(bar.getByRole("button", { name: "Log out" })).toBeVisible()
    await expect(bar.getByRole("link", { name: "Log in" })).toHaveCount(0)

    await bar.getByRole("link", { name: "Dashboard" }).click()
    await expect(page).toHaveURL(/\/en\/dashboard$/)
  })
})
