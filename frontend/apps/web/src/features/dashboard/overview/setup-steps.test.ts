import type { SpecialistResponse } from "@workspace/api"
import { describe, expect, it } from "vitest"

import { setupSteps } from "@/features/dashboard/overview/setup-steps"

const fresh: SpecialistResponse = {
  username: "rana",
  years_of_experience: null,
  primary_language: null,
  whatsapp_number: null,
  profile_picture_url: null,
}
const none = {
  specialist: fresh,
  content: {},
  credentials: [],
  locations: [],
  hours: [],
}

const done = (steps: ReturnType<typeof setupSteps>) =>
  Object.fromEntries(steps.map((step) => [step.key, step.done]))

describe("setupSteps", () => {
  it("starts with every step to do, credentials first, each linking to its place", () => {
    const steps = setupSteps(none)
    expect(done(steps)).toEqual({
      credentials: false,
      location: false,
      photo: false,
      details: false,
      about: false,
    })
    expect(steps.map((step) => step.href)).toEqual([
      "/dashboard/credentials",
      "/dashboard/locations",
      "/dashboard/profile#photos",
      "/dashboard/profile",
      "/dashboard/profile#about",
    ])
  })

  it("counts a credential in any status, and a profile picture", () => {
    const steps = setupSteps({
      ...none,
      specialist: { ...fresh, profile_picture_url: "https://cdn/p.webp" },
      credentials: [{ id: "c1", verification_status: "pending" }],
    })
    expect(done(steps)).toMatchObject({ credentials: true, photo: true })
  })

  it("needs a location with opening hours", () => {
    const place = { id: "l1", city: "Beirut", is_primary: true }
    expect(done(setupSteps({ ...none, locations: [place] })).location).toBe(
      false
    )
    expect(
      done(
        setupSteps({
          ...none,
          locations: [place],
          hours: [{ id: "h1", location_id: "l1", day_of_week: "monday" }],
        })
      ).location
    ).toBe(true)
  })

  it("needs experience, language and WhatsApp for the details", () => {
    const partly = { ...fresh, years_of_experience: 0, primary_language: "ar" }
    expect(done(setupSteps({ ...none, specialist: partly })).details).toBe(
      false
    )
    expect(
      done(
        setupSteps({
          ...none,
          specialist: { ...partly, whatsapp_number: "+96170123456" },
        })
      ).details
    ).toBe(true)
  })

  it("counts the about text once any language has a bio", () => {
    expect(
      done(setupSteps({ ...none, content: { fr: { bio: "  " } } })).about
    ).toBe(false)
    expect(
      done(setupSteps({ ...none, content: { fr: { bio: "Bonjour" } } })).about
    ).toBe(true)
  })
})
