import { expect, test } from "@playwright/test"

import { signUp, uniqueEmail } from "./support/auth"
import { chooseSyndicate, requireSyndicates } from "./support/onboarding"

test.describe("onboarding", () => {
  test("validates, then creates the profile and opens the dashboard", async ({
    page,
  }) => {
    await requireSyndicates(page)
    await signUp(page, uniqueEmail("onboard"))

    // A human-speed press: the focused field's blur must not move the button
    // away before the release (SubmitButton keeps focus in the field)
    await page
      .getByRole("button", { name: "Finish setup" })
      .click({ delay: 120 })
    await expect(page.getByText("Username is required")).toBeVisible()
    await expect(page.getByText("Syndicate is required")).toBeVisible()
    await expect(page.getByText("Member number is required")).toBeVisible()

    await page.getByLabel("Username").fill("no spaces!")
    await page.getByLabel("Username").blur()
    await expect(
      page.getByText("Use only letters, numbers, dots, dashes and underscores")
    ).toBeVisible()

    // Reserved words pass the format but the server refuses them
    await page.getByLabel("Username").fill("admin")
    await chooseSyndicate(page, /Speech Therapists/)
    await page.getByLabel("Member number").fill(`${Date.now()}`)
    await page.getByRole("button", { name: "Finish setup" }).click()
    await expect(
      page.getByText("This username isn't available. Try another.")
    ).toBeVisible()

    await page.getByLabel("Username").fill(`rana.${Date.now() % 1e8}`)
    await page.getByRole("button", { name: "Finish setup" }).click()
    await expect(page).toHaveURL(/\/en\/dashboard$/)

    // Done: onboarding sends them on
    await page.goto("/en/onboarding")
    await expect(page).toHaveURL(/\/en\/dashboard$/)
  })

  test("a taken username and member number land on their fields", async ({
    page,
    browser,
  }) => {
    await requireSyndicates(page)
    const username = `taken.${Date.now() % 1e8}`
    const memberNumber = `M-${Date.now()}`

    // First specialist takes both
    await signUp(page, uniqueEmail("first"))
    await page.getByLabel("Username").fill(username)
    await chooseSyndicate(page, /Speech Therapists/)
    await page.getByLabel("Member number").fill(memberNumber)
    await page.getByRole("button", { name: "Finish setup" }).click()
    await expect(page).toHaveURL(/\/en\/dashboard$/)

    // Second specialist, own browser context (own cookies)
    const second = await (await browser.newContext()).newPage()
    await signUp(second, uniqueEmail("second"))
    await second.getByLabel("Username").fill(username)
    await chooseSyndicate(second, /Speech Therapists/)
    await second.getByLabel("Member number").fill(memberNumber)
    await second.getByRole("button", { name: "Finish setup" }).click()
    await expect(
      second.getByText("This username is taken. Try another.")
    ).toBeVisible()

    await second.getByLabel("Username").fill(`${username}.2`)
    await second.getByRole("button", { name: "Finish setup" }).click()
    await expect(
      second.getByText(
        "This member number is already registered with this syndicate."
      )
    ).toBeVisible()
  })
})
