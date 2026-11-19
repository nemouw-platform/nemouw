import { describe, expect, it } from "vitest"

import {
  hasBio,
  toAboutValues,
  toWriteRequest,
} from "@/features/specialists/about/about-schema"

describe("about values", () => {
  it("starts empty for a language with no text yet", () => {
    expect(toAboutValues(undefined)).toEqual({
      title: "",
      headline: "",
      bio: "",
    })
  })

  it("sends only the filled fields (the rest are stored as null)", () => {
    expect(
      toWriteRequest({ title: "Orthophoniste", headline: "", bio: "Bonjour" })
    ).toEqual({ title: "Orthophoniste", bio: "Bonjour" })
    expect(toWriteRequest({ title: "", headline: "", bio: "" })).toEqual({})
  })

  it("hasBio ignores a blank bio", () => {
    expect(hasBio(undefined)).toBe(false)
    expect(hasBio({ bio: "  ", title: "Orthophoniste" })).toBe(false)
    expect(hasBio({ bio: "Bonjour" })).toBe(true)
  })
})
