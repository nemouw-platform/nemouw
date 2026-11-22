import { expect, test, type Page } from "@playwright/test"

/** Onboarding needs syndicates: load e2e/fixtures/dev-seed.sql (see README) */
export async function requireSyndicates(page: Page) {
  const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"
  const response = await page.request.get(`${apiUrl}/v1/syndicates`)
  const syndicates = (await response.json()) as unknown[]
  test.skip(
    syndicates.length === 0,
    "No syndicates: run e2e/fixtures/dev-seed.sql"
  )
}

export async function chooseSyndicate(page: Page, name: RegExp) {
  await page.getByLabel("Syndicate").click()
  await page.getByRole("option", { name }).click()
}

/**
 * Completes onboarding from the onboarding page; resolves on the dashboard
 * with the username it chose
 */
export async function onboard(page: Page): Promise<string> {
  const username = `rana.${Date.now() % 1e8}`
  await page.getByLabel("Username").fill(username)
  await chooseSyndicate(page, /Speech Therapists/)
  await page.getByLabel("Member number").fill(`M-${Date.now()}`)
  await page.getByRole("button", { name: "Finish setup" }).click()
  await expect(page).toHaveURL(/\/en\/dashboard$/)
  return username
}
