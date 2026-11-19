import { expect, test } from "@playwright/test"

import { signUp, uniqueEmail } from "./support/auth"
import {
  chooseSyndicate,
  onboard,
  requireSyndicates,
} from "./support/onboarding"

test.describe("account settings", () => {
  test.beforeEach(async ({ page }) => {
    await requireSyndicates(page)
    await signUp(page, uniqueEmail("settings"))
    await onboard(page)
    await page.getByRole("link", { name: "Settings" }).first().click()
    await expect(page).toHaveURL(/\/en\/dashboard\/settings$/)
  })

  test("change the username", async ({ page }) => {
    const username = page.getByLabel(/^Username/)

    // Reserved names pass the format but the server refuses them
    await username.fill("admin")
    await page.getByRole("button", { name: "Save username" }).click()
    await expect(
      page.getByText("This username isn't available. Try another.")
    ).toBeVisible()

    const next = `rana.new.${Date.now() % 1e8}`
    await username.fill(next)
    await page.getByRole("button", { name: "Save username" }).click()
    await expect(page.getByText("Saved", { exact: true })).toBeVisible()

    // Every page shows the new one
    await page.getByRole("link", { name: "Overview" }).first().click()
    await expect(page.getByText(`Signed in as @${next}`)).toBeVisible()
  })

  test("move to another syndicate while not verified", async ({ page }) => {
    await chooseSyndicate(page, /Occupational Therapists/)
    await page.getByLabel(/^Member number/).fill(`OT-${Date.now()}`)
    await page.getByRole("button", { name: "Save membership" }).click()
    await expect(page.getByText("Saved", { exact: true })).toBeVisible()

    await page.reload()
    await expect(page.getByLabel(/^Syndicate/)).toContainText(
      "Occupational Therapists"
    )
  })
})
