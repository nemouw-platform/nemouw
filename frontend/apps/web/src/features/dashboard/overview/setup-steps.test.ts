import type { SpecialistResponse } from "@workspace/api"
import { describe, expect, it } from "vitest"

import { setupSteps } from "@/features/dashboard/overview/setup-steps"

const fresh: SpecialistResponse = {
  username: "rana",
  years_of_experience: null,
  primary_language: null,
  whatsapp_number: null,
}

const done = (steps: ReturnType<typeof setupSteps>) =>
  Object.fromEntries(steps.map((step) => [step.key, step.done]))

describe("setupSteps", () => {
  it("starts with every step to do, each linking to its section", () => {
    const steps = setupSteps(fresh, {})
    expect(done(steps)).toEqual({ details: false, about: false })
    expect(steps.map((step) => step.href)).toEqual([
      "/dashboard/profile",
      "/dashboard/profile#about",
    ])
  })

  it("needs experience, language and WhatsApp for the details", () => {
    const partly = { ...fresh, years_of_experience: 0, primary_language: "ar" }
    expect(done(setupSteps(partly, {})).details).toBe(false)
    expect(
      done(setupSteps({ ...partly, whatsapp_number: "+961 70 123 456" }, {}))
        .details
    ).toBe(true)
  })

  it("counts the about text once any language has a bio", () => {
    expect(done(setupSteps(fresh, { fr: { bio: "  " } })).about).toBe(false)
    expect(
      done(
        setupSteps(fresh, { fr: { title: "Orthophoniste", bio: "Bonjour" } })
      ).about
    ).toBe(true)
  })
})
