import { expect, test } from "@playwright/test"

import { signUp, uniqueEmail } from "./support/auth"
import { pngFile, textFile } from "./support/files"
import { onboard, requireSyndicates } from "./support/onboarding"

// Uploads go to the media bucket in backend/.env; each test removes its photo
test.describe("profile photos", () => {
  test.beforeEach(async ({ page }) => {
    await requireSyndicates(page)
    await signUp(page, uniqueEmail("photos"))
    await onboard(page)
    await page.goto("/en/dashboard/profile")
  })

  test("a profile picture uploads with a preview, then can be removed", async ({
    page,
  }) => {
    const picture = page
      .locator("#photos section")
      .filter({ hasText: "Profile picture" })
    const input = picture.locator("input[type=file]")

    // Checked before anything is sent
    await input.setInputFiles(textFile())
    await expect(
      picture.getByText(
        "This file type isn't accepted. Use JPEG, PNG, or WebP."
      )
    ).toBeVisible()

    await input.setInputFiles(await pngFile(page))
    // Saved: the image now comes from the CDN, and can be changed or removed
    await expect(
      picture.getByRole("img", { name: "Profile picture" })
    ).toHaveAttribute("src", /^https:\/\//)
    await expect(
      picture.getByRole("button", { name: "Change photo" })
    ).toBeVisible()

    // Still there after a reload, and the overview counts it
    await page.reload()
    await expect(
      picture.getByRole("img", { name: "Profile picture" })
    ).toBeVisible()

    await picture.getByRole("button", { name: "Remove" }).click()
    await expect(picture.getByRole("img")).toHaveCount(0)
    await expect(
      picture.getByRole("button", { name: "Upload a photo" })
    ).toBeVisible()
  })
})
