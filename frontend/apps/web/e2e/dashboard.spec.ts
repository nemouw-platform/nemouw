import { expect, test } from "@playwright/test"

import { signUp, uniqueEmail } from "./support/auth"
import { onboard, requireSyndicates } from "./support/onboarding"

test.describe("specialist dashboard", () => {
  test.beforeEach(async ({ page }) => {
    await requireSyndicates(page)
    await signUp(page, uniqueEmail("dashboard"))
    await onboard(page)
  })

  test("shows the verification status and what is left to set up", async ({
    page,
  }) => {
    await expect(
      page.getByRole("heading", { name: "Welcome back" })
    ).toBeVisible()
    await expect(page.getByText("Pending", { exact: true })).toBeVisible()
    await expect(page.getByText("0 of 2 done")).toBeVisible()

    await page.getByRole("link", { name: /Add your practice details/ }).click()
    await expect(page).toHaveURL(/\/en\/dashboard\/profile$/)
    await expect(
      page.getByRole("heading", { name: "Your profile" })
    ).toBeVisible()
  })

  test("saves the practice details and the text in a language", async ({
    page,
  }) => {
    await page.goto("/en/dashboard/profile")

    // Checked in the browser before anything is sent
    await page.getByLabel("WhatsApp number").fill("70")
    await page.getByLabel("Youngest age you see").fill("12")
    await page.getByLabel("Oldest age you see").fill("3")
    await page.getByRole("button", { name: "Save details" }).click()
    await expect(
      page.getByText("Enter a valid phone number for this country")
    ).toBeVisible()
    await expect(
      page.getByText("Must be at least the youngest age")
    ).toBeVisible()

    // No geo-IP header locally: the field starts on Lebanon
    await page.getByLabel("WhatsApp number").fill("70123456")
    await page.getByLabel("Oldest age you see").fill("18")
    await page.getByLabel("Years of experience").fill("8")
    await page.getByLabel("Main language").click()
    await page.getByRole("option", { name: "Arabic" }).click()
    await page.getByRole("button", { name: "Save details" }).click()
    await expect(page.getByRole("status")).toHaveText("Saved")

    // The English tab is open on an English page. The other languages' tabs
    // stay mounted (hidden), so look inside the visible panel only
    const english = page.getByRole("tabpanel", { name: /English/ })
    const bio = english.getByRole("textbox", { name: "Bio" })
    await bio.fill("I help children find their words.")
    await english.getByRole("button", { name: "Save English text" }).click()
    await expect(english.getByRole("status")).toHaveText("Saved")

    // Both stick after a reload, and the overview counts them
    await page.reload()
    await expect(page.getByLabel("Years of experience")).toHaveValue("8")
    await expect(bio).toHaveValue("I help children find their words.")
    await page.getByRole("link", { name: "Overview" }).first().click()
    await expect(page.getByText("2 of 2 done")).toBeVisible()
    await expect(
      page.getByRole("heading", { name: "Your profile is complete" })
    ).toBeVisible()
  })

  test("the phone field starts on the visitor's country, found on the server", async ({
    page,
  }) => {
    // What Vercel adds from the visitor's IP; only on the page request, so
    // the API calls (another origin) are untouched
    await page.route("**/en/dashboard/profile", (route) =>
      route.continue({
        headers: { ...route.request().headers(), "x-vercel-ip-country": "FR" },
      })
    )
    await page.goto("/en/dashboard/profile")
    const country = page.getByRole("combobox", { name: /^Country:/ })
    await expect(country).toHaveAccessibleName("Country: France (+33)")
    await expect(page.getByLabel("WhatsApp number")).toHaveAttribute(
      "placeholder",
      /^06/
    )

    // Search by name and pick another country; its flag loads
    await country.click()
    await page.getByRole("combobox", { name: "Search countries" }).fill("leban")
    await page.getByRole("option", { name: /Lebanon/ }).click()
    await expect(country).toHaveAccessibleName("Country: Lebanon (+961)")
    // Ready to type the number
    await expect(page.getByLabel("WhatsApp number")).toBeFocused()
    await expect(country.locator("img")).toHaveAttribute("src", "/flags/LB.svg")
    expect(
      await country
        .locator("img")
        .evaluate((img: HTMLImageElement) => img.naturalWidth)
    ).toBeGreaterThan(0)

    // A pasted international number picks its own country
    await page.getByLabel("WhatsApp number").fill("+33 6 12 34 56 78")
    await expect(country).toHaveAccessibleName("Country: France (+33)")
    await expect(page.getByLabel("WhatsApp number")).toHaveValue(
      "06 12 34 56 78"
    )
  })

  test("logs out to the login page", async ({ page }) => {
    await page.getByRole("button", { name: "Log out" }).click()
    await expect(page).toHaveURL(/\/en\/login$/)

    await page.goto("/en/dashboard")
    await expect(page).toHaveURL(/\/en\/login\?next=%2Fdashboard$/)
  })
})
