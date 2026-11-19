import { expect, test } from "@playwright/test"

import { signUp, uniqueEmail } from "./support/auth"
import { onboard, requireSyndicates } from "./support/onboarding"

test.describe("conditions", () => {
  test.beforeEach(async ({ page }) => {
    await requireSyndicates(page)
    await signUp(page, uniqueEmail("conditions"))
    await onboard(page)
    await page.getByRole("link", { name: "Conditions" }).first().click()
    await expect(page).toHaveURL(/\/en\/dashboard\/conditions$/)
  })

  test("search, pick and save the conditions treated", async ({ page }) => {
    // The list comes from the reference data (e2e/fixtures/dev-seed.sql)
    await expect(page.getByText("None selected")).toBeVisible()

    // Search narrows the list, ignoring case
    await page.getByLabel("Search conditions").fill("STUTT")
    await expect(page.getByRole("checkbox")).toHaveCount(1)
    await page.getByRole("checkbox", { name: "Stuttering" }).click()

    await page.getByLabel("Search conditions").fill("")
    await page.getByRole("checkbox", { name: "Dyslexia" }).click()
    await expect(page.getByText("2 selected")).toBeVisible()

    await page.getByRole("button", { name: "Save conditions" }).click()
    await expect(page.getByText("Saved", { exact: true })).toBeVisible()

    // Saved: still picked after a reload, in any language
    await page.goto("/fr/dashboard/conditions")
    await expect(
      page.getByRole("checkbox", { name: "Bégaiement" })
    ).toBeChecked()
    await expect(page.getByRole("checkbox", { name: "Dyslexie" })).toBeChecked()

    // The overview counts it
    await page.goto("/en/dashboard")
    await expect(
      page.getByRole("link", {
        name: /Choose the conditions you treat \(done\)/,
      })
    ).toBeVisible()
  })
})
