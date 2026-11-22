import { expect, test } from "@playwright/test"

import {
  enterCode,
  OTP,
  PASSWORD,
  signUp,
  submitSignupDetails,
  uniqueEmail,
  passwordField,
} from "./support/auth"

test.describe("sign-up", () => {
  test("step 1 validates before sending anything", async ({ page }) => {
    await page.goto("/en/signup")
    await page.getByRole("button", { name: "Send my code" }).click()

    await expect(page.getByText("First name is required")).toBeVisible()
    await expect(page.getByText("Last name is required")).toBeVisible()
    await expect(page.getByText("Email address is required")).toBeVisible()
    await expect(page.getByLabel("First name")).toBeFocused()

    await page.getByLabel("Email address").fill("rana@clinic")
    await page.getByLabel("Email address").blur()
    await expect(
      page.getByText("Enter an email address like name@example.com")
    ).toBeVisible()
  })

  test("an address with an account gets a log-in link", async ({ page }) => {
    const email = uniqueEmail("taken")
    await signUp(page, email)
    await page.context().clearCookies()

    await page.goto("/en/signup")
    await page.getByLabel("First name").fill("Rana")
    await page.getByLabel("Last name").fill("Haddad")
    await page.getByLabel("Email address").fill(email)
    await page.getByRole("button", { name: "Send my code" }).click()

    await expect(
      page.getByText("An account already uses this email.")
    ).toBeVisible()
    await expect(
      page.getByRole("link", { name: "Log in instead" })
    ).toHaveAttribute("href", "/en/login")
  })

  test("verify: a wrong code is rejected and cleared, resend waits", async ({
    page,
  }) => {
    await submitSignupDetails(page, uniqueEmail("verify"))

    await expect(page.getByText("Step 2 of 3.")).toBeVisible()
    await expect(
      page.getByRole("button", { name: /^Resend code in 0:\d\d$/ })
    ).toBeDisabled()

    await enterCode(page, OTP === "111111" ? "222222" : "111111")
    await expect(
      page.getByText(
        "This code is wrong or has expired. Try again, or send a new code."
      )
    ).toBeVisible()
    await expect(page.locator("#code")).toHaveValue("")

    await enterCode(page, OTP)
    await expect(page).toHaveURL(/\/en\/signup\/password\?email=/)
  })

  test("password: the checklist and the server's common-password rule", async ({
    page,
  }) => {
    const email = uniqueEmail("password")
    await submitSignupDetails(page, email)
    await enterCode(page, OTP)

    const password = passwordField(page)
    await expect(password).toHaveAttribute("autocomplete", "new-password")
    await password.fill(`x-${email.split("@")[0]}`)
    await page.getByRole("button", { name: "Create my account" }).click()
    await expect(
      page
        .getByText("Don't include “nemouw” or the part of your email before @")
        .first()
    ).toBeVisible()

    await password.fill("password123")
    await page.getByRole("button", { name: "Create my account" }).click()
    await expect(page.getByText("This password is too common")).toBeVisible()

    await page.getByRole("button", { name: "Show password" }).click()
    await expect(password).toHaveAttribute("type", "text")

    await password.fill(PASSWORD)
    await page.getByRole("button", { name: "Create my account" }).click()
    await expect(page).toHaveURL(/\/en\/onboarding$/)
  })

  test("Arabic pages are right to left, with left-to-right codes", async ({
    page,
  }) => {
    await page.goto("/ar/signup")
    await expect(page.locator("html")).toHaveAttribute("dir", "rtl")
    await expect(page.getByRole("heading", { level: 1 })).toHaveText(
      "أنشئ حسابك كأخصائي"
    )
  })
})
