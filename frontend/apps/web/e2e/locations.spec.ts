import { expect, test } from "@playwright/test"

import { signUp, uniqueEmail } from "./support/auth"
import { onboard, requireSyndicates } from "./support/onboarding"

test.describe("locations and hours", () => {
  test.beforeEach(async ({ page }) => {
    await requireSyndicates(page)
    await signUp(page, uniqueEmail("locations"))
    await onboard(page)
    await page.getByRole("link", { name: "Locations & hours" }).first().click()
    await expect(page).toHaveURL(/\/en\/dashboard\/locations$/)
  })

  test("add a location with its hours, then a new main one", async ({
    page,
  }) => {
    await expect(page.getByText("No locations yet")).toBeVisible()
    await page.getByRole("button", { name: "Add a location" }).click()

    await page.getByRole("button", { name: "Add location" }).click()
    await expect(page.getByText("City is required")).toBeVisible()

    await page.getByLabel("Name", { exact: true }).fill("Hamra clinic")
    await page.getByLabel(/^City/).fill("Beirut")
    await page.getByLabel("Address", { exact: true }).fill("Bliss Street")
    // No geo-IP header locally: the phone field starts on Lebanon
    await page.getByLabel("Phone", { exact: true }).fill("01 123 456")
    await page.getByRole("button", { name: "Add location" }).click()

    // The popup moves straight on to its opening hours
    await expect(page.getByText("Opening hours at Hamra clinic")).toBeVisible()
    await page.getByRole("button", { name: "Add hours on Monday" }).click()
    await expect(page.getByLabel("Monday, opens")).toHaveValue("09:00")
    await expect(page.getByLabel("Monday, closes")).toHaveValue("17:00")

    // Checked before anything is sent: reversed, missing and overlapping times
    await page.getByRole("button", { name: "Add hours on Tuesday" }).click()
    await page.getByLabel("Tuesday, opens").fill("17:00")
    await page.getByLabel("Tuesday, closes").fill("09:00")
    await page.getByRole("button", { name: "Add hours on Monday" }).click()
    await page.getByRole("button", { name: "Save hours" }).click()
    await expect(page.getByText("Must close after it opens")).toBeVisible()
    await expect(page.getByText("Fill in both times")).toBeVisible()

    await page.getByLabel("Tuesday, opens").fill("09:00")
    await page.getByLabel("Tuesday, closes").fill("13:00")
    await page.getByLabel("Monday, opens").nth(1).fill("16:00")
    await page.getByLabel("Monday, closes").nth(1).fill("19:00")
    await page.getByRole("button", { name: "Save hours" }).click()
    await expect(page.getByText("Overlaps other hours that day")).toBeVisible()

    await page.getByLabel("Monday, opens").nth(1).fill("17:30")
    await page.getByRole("button", { name: "Save hours" }).click()
    // Saved: the card shows the week (each window is its own call)
    await expect(page.getByText("09:00–17:00, 17:30–19:00")).toBeVisible()

    // Saved, and still there after a reload
    await page.reload()
    await expect(
      page.getByRole("heading", { name: "Hamra clinic" })
    ).toBeVisible()
    await expect(page.getByText("Main location", { exact: true })).toBeVisible()
    await expect(page.getByText("09:00–17:00, 17:30–19:00")).toBeVisible()
    await expect(page.getByText("09:00–13:00")).toBeVisible()
    await expect(page.getByText("+961 1 123 456")).toBeVisible()

    // Editing opens the same popup, with the saved hours in it
    await page.getByRole("button", { name: "Opening hours" }).click()
    const dialog = page.getByRole("dialog")
    await expect(dialog.getByLabel("Tuesday, opens")).toHaveValue("09:00")
    await dialog.getByRole("button", { name: "Cancel" }).click()
    await expect(dialog).toHaveCount(0)

    // The overview counts it
    await page.getByRole("link", { name: "Overview" }).first().click()
    await expect(
      page.getByRole("link", { name: /Add where and when you work \(done\)/ })
    ).toBeVisible()
    await page.getByRole("link", { name: "Locations & hours" }).first().click()

    // A second location, made the main one
    await page.getByRole("button", { name: "Add a location" }).click()
    await page.getByLabel(/^City/).fill("Jounieh")
    await page
      .getByRole("checkbox", { name: "Make this my main location" })
      .click()
    await page.getByRole("button", { name: "Add location" }).click()
    // Saved: its hours editor opens; leave the hours for later
    await expect(page.getByText("Opening hours at Jounieh")).toBeVisible()
    await page.getByRole("button", { name: "Set hours later" }).click()

    // Visible only: Next keeps the Overview visited before mounted, hidden
    const cards = page.locator("main [data-slot=card]:visible")
    await expect(cards).toHaveCount(2)
    // The main one comes first, and cannot be removed while others exist
    await expect(cards.first()).toContainText("Jounieh")
    await expect(cards.first()).toContainText("Main location")
    await expect(
      cards.first().getByRole("button", { name: "Remove" })
    ).toBeDisabled()

    // Remove the old one, after confirming
    await cards.nth(1).getByRole("button", { name: "Remove" }).click()
    await page
      .getByRole("alertdialog")
      .getByRole("button", { name: "Remove" })
      .click()
    await expect(cards).toHaveCount(1)
    await expect(
      page.getByRole("heading", { name: "Hamra clinic" })
    ).toHaveCount(0)
  })
})
