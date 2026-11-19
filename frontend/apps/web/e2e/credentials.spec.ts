import { expect, test } from "@playwright/test"

import { signUp, uniqueEmail } from "./support/auth"
import { pdfFile, textFile } from "./support/files"
import { onboard, requireSyndicates } from "./support/onboarding"

// Documents go to the media bucket in backend/.env; the test withdraws its
// credential, which deletes the document
test.describe("credentials", () => {
  test.beforeEach(async ({ page }) => {
    await requireSyndicates(page)
    await signUp(page, uniqueEmail("credentials"))
    await onboard(page)
    await page.getByRole("link", { name: "Credentials" }).first().click()
    await expect(page).toHaveURL(/\/en\/dashboard\/credentials$/)
  })

  test("add, view, edit and withdraw a credential", async ({ page }) => {
    await expect(page.getByText("No credentials yet")).toBeVisible()
    await page.getByRole("button", { name: "Add a credential" }).click()

    // Checked in the browser before anything is sent
    await page.getByRole("button", { name: "Send for review" }).click()
    for (const message of [
      "Type is required",
      "Title is required",
      "Issued by is required",
      "Document is required",
    ]) {
      await expect(page.getByText(message)).toBeVisible()
    }

    await page.getByLabel("Type").click()
    await page.getByRole("option", { name: "Licence" }).click()
    await page.getByLabel("Title").fill("Licence to practise")
    await page.getByLabel("Issued by").fill("Order of Speech Therapists")
    await page.getByLabel("Issue date").fill("2020-05-03")
    await page.getByLabel("Expiry date").fill("2019-01-01")

    // The document: a wrong type is refused, a PDF shows with its icon
    const document = page.getByLabel("Document")
    await document.setInputFiles(textFile())
    await expect(
      page.getByText(
        "This file type isn't accepted. Use JPEG, PNG, WebP, or PDF."
      )
    ).toBeVisible()
    await document.setInputFiles(pdfFile())
    await expect(page.getByText("licence.pdf")).toBeVisible()

    await page.getByRole("button", { name: "Send for review" }).click()
    await expect(
      page.getByText("Must be on or after the issue date")
    ).toBeVisible()
    await page.getByLabel("Expiry date").fill("2030-01-01")
    await page.getByRole("button", { name: "Send for review" }).click()

    // Submitted for review
    const card = page.getByRole("heading", { name: "Licence to practise" })
    await expect(card).toBeVisible()
    await expect(page.getByText("In review")).toBeVisible()

    // The private document opens in a new tab, through a short-lived link
    const popup = page.waitForEvent("popup")
    await page.getByRole("button", { name: "View document" }).click()
    const tab = await popup
    await expect.poll(() => tab.url()).toContain("X-Amz-Signature")
    await tab.close()

    // Edit: the saved document shows, kept unless replaced
    await page.getByRole("button", { name: "Edit" }).click()
    await expect(page.getByText("PDF document")).toBeVisible()
    await page.getByLabel("Title").fill("Licence to practise speech therapy")
    await page.getByRole("button", { name: "Save" }).click()
    await expect(
      page.getByRole("heading", { name: "Licence to practise speech therapy" })
    ).toBeVisible()

    // The overview counts it
    await page.getByRole("link", { name: "Overview" }).first().click()
    await expect(
      page.getByRole("link", { name: /Add your credentials \(done\)/ })
    ).toBeVisible()

    // Withdraw, after confirming
    await page.getByRole("link", { name: "Credentials" }).first().click()
    await page.getByRole("button", { name: "Withdraw" }).click()
    await page
      .getByRole("alertdialog")
      .getByRole("button", { name: "Withdraw" })
      .click()
    await expect(page.getByText("No credentials yet")).toBeVisible()
  })
})
