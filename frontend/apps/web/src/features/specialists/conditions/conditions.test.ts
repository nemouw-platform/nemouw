import type { Condition } from "@workspace/api"
import { describe, expect, it } from "vitest"

import {
  conditionLabel,
  matchesCondition,
  sameSelection,
  sortConditions,
} from "@/features/specialists/conditions/conditions"

const stuttering: Condition = {
  id: "s",
  code: "stuttering",
  label: "Stuttering",
  label_fr: "Bégaiement",
  label_ar: "التأتأة",
}
const autism: Condition = {
  id: "a",
  code: "autism",
  label: "Autism spectrum disorder",
  label_fr: "Trouble du spectre de l'autisme",
  label_ar: "",
}

describe("conditionLabel", () => {
  it("names a condition in the page's language", () => {
    expect(conditionLabel(stuttering, "en")).toBe("Stuttering")
    expect(conditionLabel(stuttering, "fr")).toBe("Bégaiement")
    expect(conditionLabel(stuttering, "ar")).toBe("التأتأة")
  })

  it("falls back to English when a translation is missing", () => {
    expect(conditionLabel(autism, "ar")).toBe("Autism spectrum disorder")
  })
})

describe("sortConditions / matchesCondition", () => {
  it("sorts by the name in the page's language", () => {
    expect(sortConditions([stuttering, autism], "en").map((c) => c.id)).toEqual(
      ["a", "s"]
    )
    expect(sortConditions([autism, stuttering], "fr").map((c) => c.id)).toEqual(
      ["s", "a"]
    )
  })

  it("finds by either name, ignoring accents and case", () => {
    expect(matchesCondition(stuttering, "begai", "fr")).toBe(true)
    expect(matchesCondition(stuttering, "STUTT", "fr")).toBe(true)
    expect(matchesCondition(stuttering, "autism", "fr")).toBe(false)
    expect(matchesCondition(stuttering, "  ", "en")).toBe(true)
  })
})

describe("sameSelection", () => {
  it("ignores order", () => {
    expect(sameSelection(["a", "b"], ["b", "a"])).toBe(true)
    expect(sameSelection(["a"], ["a", "b"])).toBe(false)
    expect(sameSelection(["a", "c"], ["a", "b"])).toBe(false)
  })
})
