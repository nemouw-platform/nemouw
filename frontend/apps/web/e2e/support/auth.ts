import { expect, type Page } from "@playwright/test"

import { readResetToken } from "./mailbox"

/** The backend's DEV_FIXED_OTP (backend/.env); every sign-up code is this */
export const OTP = process.env.E2E_OTP ?? "000000"

/** A password that passes every backend rule */
export const PASSWORD = "violet-harbor-lantern-42"

/**
 * The password input. A whole-name match, because "Show password" (eye
 * button) and "Password requirements" (checklist) also contain the word, and
 * the label's name ends with the required-field asterisk.
 */
export function passwordField(page: Page) {
  return page.getByLabel(/^Password\*?$/)
}

/** A fresh address per test, so throttles and "email taken" never collide */
export function uniqueEmail(tag: string): string {
  return `e2e-${tag}-${Date.now()}-${Math.floor(Math.random() * 1e4)}@example.com`
}

/** Step 1 of sign-up; resolves on the code step */
export async function submitSignupDetails(page: Page, email: string) {
  await page.goto("/en/signup")
  await page.getByLabel("First name").fill("Rana")
  await page.getByLabel("Last name").fill("Haddad")
  await page.getByLabel("Email address").fill(email)
  await page.getByRole("button", { name: "Send my code" }).click()
  await expect(page).toHaveURL(/\/en\/signup\/verify\?email=/)
}

/** Types the code into the six boxes (the form submits itself when full) */
export async function enterCode(page: Page, code: string) {
  await page.locator("#code").fill(code)
}

/** The whole sign-up; resolves signed in, on onboarding */
export async function signUp(page: Page, email: string, password = PASSWORD) {
  await submitSignupDetails(page, email)
  await enterCode(page, OTP)
  await expect(page).toHaveURL(/\/en\/signup\/password/)
  await passwordField(page).fill(password)
  await page.getByRole("button", { name: "Create my account" }).click()
  await expect(page).toHaveURL(/\/en\/onboarding$/)
}

/** Logs in from the login page currently shown (or /en/login) */
export async function logIn(page: Page, email: string, password = PASSWORD) {
  if (!page.url().includes("/login")) await page.goto("/en/login")
  await page.getByLabel("Email address").fill(email)
  await passwordField(page).fill(password)
  await page.getByRole("button", { name: "Log in" }).click()
}

/** Requests a reset link and returns the token from the emailed link */
export async function requestResetToken(page: Page, email: string) {
  await page.goto("/en/forgot-password")
  await page.getByLabel("Email address").fill(email)
  await page.getByRole("button", { name: "Send reset link" }).click()
  await expect(
    page.getByRole("heading", { name: "Check your email" })
  ).toBeVisible()
  return readResetToken(email)
}
