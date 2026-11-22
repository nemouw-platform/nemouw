import { expect, test, type Page } from "@playwright/test"

import { signUp, uniqueEmail } from "./support/auth"
import { pdfFile } from "./support/files"
import { onboard, requireSyndicates } from "./support/onboarding"
import { logInAsOwner, OWNER_EMAIL } from "./support/syndicate"

/** A specialist of the seeded syndicate who sent one credential; their username */
async function specialistWithCredential(page: Page): Promise<string> {
  await signUp(page, uniqueEmail("reviewed"))
  const username = await onboard(page)
  await page.getByRole("link", { name: "Credentials" }).first().click()
  await page.getByRole("button", { name: "Add a credential" }).click()
  await page.getByLabel("Type").click()
  await page.getByRole("option", { name: "Licence" }).click()
  await page.getByLabel("Title").fill("Licence to practise")
  await page.getByLabel("Issued by").fill("Order of Speech Therapists")
  await page.getByLabel(/^Document/).setInputFiles(pdfFile())
  await page.getByRole("button", { name: "Send for review" }).click()
  await expect(page.getByText("In review")).toBeVisible()
  return username
}

// Needs the seeded syndicate owner: load e2e/fixtures/dev-seed.sql
test.describe("syndicate", () => {
  test.beforeEach(async ({ page }) => requireSyndicates(page))

  test("review a specialist and their credential", async ({ page }) => {
    const username = await specialistWithCredential(page)
    await logInAsOwner(page)

    // The queue: the new specialist waits, with one credential to review
    const row = page.getByRole("link", { name: new RegExp(`@${username}`) })
    await expect(row).toContainText("1 credential to review")
    await row.click()
    await expect(page).toHaveURL(/\/en\/syndicate\/specialist\?id=/)
    // The page shown: Next keeps visited ones in the DOM, hidden
    const main = page.locator("main > div:visible")
    await expect(main.getByText(`@${username}`)).toHaveCount(1)

    // The credential: its document opens, then it is verified
    const popup = page.waitForEvent("popup")
    await main.getByRole("button", { name: "View document" }).click()
    const tab = await popup
    await expect.poll(() => tab.url()).toContain("X-Amz-Signature")
    await tab.close()
    await main.getByRole("button", { name: "Verify", exact: true }).click()
    await expect(main.getByText("Verified", { exact: true })).toBeVisible()

    // Rejecting asks why; the reason shows
    await main.getByRole("button", { name: "Reject specialist" }).click()
    const dialog = page.getByRole("dialog")
    await dialog.getByRole("button", { name: "Reject specialist" }).click()
    await expect(dialog.getByText("Reason is required")).toBeVisible()
    await dialog.getByLabel("Reason").fill("The member number isn't ours.")
    await dialog.getByRole("button", { name: "Reject specialist" }).click()
    await expect(dialog).toBeHidden()
    await expect(
      main.getByText("You rejected them: The member number isn't ours.")
    ).toBeVisible()

    // Verified after all
    await main.getByRole("button", { name: "Verify specialist" }).click()
    await expect(
      main.getByText("Verified: patients can find them in search.")
    ).toBeVisible()

    // Suspended, then back
    await main.getByRole("button", { name: "Suspend" }).click()
    await page
      .getByRole("alertdialog")
      .getByRole("button", { name: "Suspend" })
      .click()
    await expect(main.getByText(/This account is suspended/)).toBeVisible()
    await main.getByRole("button", { name: "Lift suspension" }).click()
    await expect(main.getByText(/This account is suspended/)).toBeHidden()

    // Back on the queue, they moved to "Verified"
    await main.getByRole("link", { name: "All reviews" }).click()
    await expect(page).toHaveURL(/\/en\/syndicate$/)
    const queue = page.locator("main > div:visible")
    await queue.getByRole("tab", { name: "Verified" }).click()
    await expect(
      queue.getByRole("link", { name: new RegExp(`@${username}`) })
    ).toContainText("Credentials reviewed")
  })

  test("invite, rename and remove staff", async ({ page }) => {
    await logInAsOwner(page)
    await page.getByRole("link", { name: "Staff" }).first().click()
    await expect(page).toHaveURL(/\/en\/syndicate\/staff$/)
    const main = page.locator("main > div:visible")
    await expect(main.getByText("Rana Haddad")).toBeVisible()
    await expect(main.getByText("Owner", { exact: true })).toBeVisible()

    // An address that already has an account is refused
    await main.getByRole("button", { name: "Invite staff" }).click()
    const dialog = page.getByRole("dialog")
    await dialog.getByLabel("First name").fill("Karim")
    await dialog.getByLabel("Last name").fill("Nassar")
    await dialog.getByLabel("Email").fill(OWNER_EMAIL)
    await dialog.getByLabel("Title").fill("Registrar")
    await dialog.getByRole("button", { name: "Send invite" }).click()
    await expect(
      dialog.getByText("This email already has a Nemouw account.")
    ).toBeVisible()

    const email = uniqueEmail("staff")
    await dialog.getByLabel("Email").fill(email)
    await dialog.getByRole("button", { name: "Send invite" }).click()
    await expect(dialog).toBeHidden()
    const member = main.getByRole("listitem").filter({ hasText: email })
    await expect(member).toContainText("Invite sent")

    // A new title
    await member.getByRole("button", { name: "Edit" }).click()
    await dialog.getByLabel("Title").fill("Secretary general")
    await dialog.getByRole("button", { name: "Save" }).click()
    await expect(dialog).toBeHidden()
    await expect(member).toContainText("Secretary general")

    // Removed
    await member.getByRole("button", { name: "Remove" }).click()
    await page
      .getByRole("alertdialog")
      .getByRole("button", { name: "Remove" })
      .click()
    await expect(member).toBeHidden()
  })

  test("edit the syndicate's contact details", async ({ page }) => {
    await logInAsOwner(page)
    await page.getByRole("link", { name: "Syndicate details" }).first().click()
    await expect(page).toHaveURL(/\/en\/syndicate\/settings$/)
    const main = page.locator("main > div:visible")

    const email = uniqueEmail("contact")
    await main.getByLabel("Contact email").fill("not an email")
    await main.getByRole("button", { name: "Save" }).click()
    await expect(
      main.getByText("Enter an email address like name@example.com")
    ).toBeVisible()
    await main.getByLabel("Contact email").fill(email)
    await main.getByLabel("Contact phone").fill("71 123 456")
    await main.getByRole("button", { name: "Save" }).click()
    await expect(main.getByText("Saved")).toBeVisible()

    // The form starts again from what was saved
    await expect(main.getByLabel("Contact email")).toHaveValue(email)
    await expect(
      main.getByText("Leave empty to keep the current one")
    ).toHaveCount(0)
  })
})
