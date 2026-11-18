import type { SpecialistResponse } from "@workspace/api"
import { describe, expect, it } from "vitest"

import {
  createDetailsSchema,
  toDetailsValues,
  toUpdateRequest,
  type DetailsValues,
} from "@/features/specialists/details/details-schema"
import type { FormErrorsTranslator } from "@/lib/forms/api-errors"

const t = ((key: string) => key) as FormErrorsTranslator
const schema = createDetailsSchema(t, {
  phoneFormat: "phoneFormat",
  ageRange: "ageRange",
})

const empty: DetailsValues = {
  years_of_experience: "",
  gender: "",
  primary_language: "",
  whatsapp_number: "",
  min_age_treated: "",
  max_age_treated: "",
}

const issues = (values: Partial<DetailsValues>) => {
  const result = schema.safeParse({ ...empty, ...values })
  return result.success
    ? {}
    : Object.fromEntries(
        result.error.issues.map((issue) => [issue.path[0], issue.message])
      )
}

describe("createDetailsSchema", () => {
  it("accepts an empty form: every field is optional", () => {
    expect(issues({})).toEqual({})
  })

  it("needs a complete, valid international WhatsApp number", () => {
    expect(issues({ whatsapp_number: "+96170123456" })).toEqual({})
    for (const phone of ["+9617", "+961123", "70123456"]) {
      expect(issues({ whatsapp_number: phone })).toEqual({
        whatsapp_number: "phoneFormat",
      })
    }
  })

  it("puts a reversed age range on the oldest age", () => {
    expect(issues({ min_age_treated: "12", max_age_treated: "3" })).toEqual({
      max_age_treated: "ageRange",
    })
    expect(issues({ min_age_treated: "3", max_age_treated: "3" })).toEqual({})
    expect(issues({ min_age_treated: "12" })).toEqual({})
  })
})

describe("toDetailsValues / toUpdateRequest", () => {
  const saved: SpecialistResponse = {
    years_of_experience: 0,
    gender: null,
    primary_language: "ar",
    whatsapp_number: "+96170123456",
    min_age_treated: 2,
    max_age_treated: null,
  }

  it("shows nulls as empty fields and numbers as text (0 included)", () => {
    expect(toDetailsValues(saved)).toEqual({
      years_of_experience: "0",
      gender: "",
      primary_language: "ar",
      whatsapp_number: "+96170123456",
      min_age_treated: "2",
      max_age_treated: "",
    })
  })

  it("sends every field, empty ones as null so they are cleared", () => {
    expect(toUpdateRequest(toDetailsValues(saved))).toEqual({
      years_of_experience: 0,
      gender: null,
      primary_language: "ar",
      whatsapp_number: "+96170123456",
      min_age_treated: 2,
      max_age_treated: null,
    })
  })
})
