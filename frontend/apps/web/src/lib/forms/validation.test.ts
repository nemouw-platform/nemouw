import { describe, expect, it } from "vitest"

import type { FormErrorsTranslator } from "@/lib/forms/api-errors"
import { createFieldRules } from "@/lib/forms/validation"

/** Translator stub that echoes the key and values */
const t = ((key: string, values?: object) =>
  values ? `${key} ${JSON.stringify(values)}` : key) as FormErrorsTranslator

const rules = createFieldRules(t)
const errorOf = (result: {
  success: boolean
  error?: { issues: Array<{ message: string }> }
}) => (result.success ? null : result.error!.issues[0]!.message)

describe("createFieldRules", () => {
  it("text: '{label} is required' for blank or whitespace input", () => {
    expect(errorOf(rules.text("First name", 5).safeParse("   "))).toBe(
      'required {"field":"First name"}'
    )
  })

  it("text: trims and enforces the max length", () => {
    expect(rules.text("First name", 5).parse("  Rana ")).toBe("Rana")
    expect(errorOf(rules.text("First name", 5).safeParse("Ranaaa"))).toBe(
      'tooLong {"max":5}'
    )
  })

  it("optionalText: trims, allows blank, enforces the max length", () => {
    expect(rules.optionalText(5).parse("   ")).toBe("")
    expect(rules.optionalText(5).parse(" Rana ")).toBe("Rana")
    expect(errorOf(rules.optionalText(5).safeParse("Ranaaa"))).toBe(
      'tooLong {"max":5}'
    )
  })

  it("optionalCount: blank or a whole number up to the max", () => {
    const years = rules.optionalCount(70)
    expect(years.parse("")).toBe("")
    expect(years.parse(" 12 ")).toBe("12")
    expect(errorOf(years.safeParse("1.5"))).toBe("wholeNumber")
    expect(errorOf(years.safeParse("-3"))).toBe("wholeNumber")
    expect(errorOf(years.safeParse("71"))).toBe('tooLarge {"max":70}')
  })

  it("optionalAmount: blank, or money with up to 2 decimals and a cap", () => {
    const price = rules.optionalAmount(100)
    expect(price.parse("")).toBe("")
    expect(price.parse(" 40.5 ")).toBe("40.5")
    expect(price.parse("40,50")).toBe("40,50")
    expect(errorOf(price.safeParse("4.505"))).toBe("amount")
    expect(errorOf(price.safeParse("forty"))).toBe("amount")
    expect(errorOf(price.safeParse("100.01"))).toBe('tooLarge {"max":100}')
  })

  it("email: required, then the backend's shape", () => {
    expect(errorOf(rules.email("Email").safeParse(""))).toBe(
      'required {"field":"Email"}'
    )
    expect(errorOf(rules.email("Email").safeParse("rana@clinic"))).toBe(
      "field.MUST_BE_EMAIL"
    )
    expect(rules.email("Email").parse(" rana@clinic.lb ")).toBe(
      "rana@clinic.lb"
    )
  })

  it("digits: required when empty, 'enter all N' when incomplete", () => {
    const code = rules.digits("Code", 6)
    expect(errorOf(code.safeParse(""))).toBe('required {"field":"Code"}')
    expect(errorOf(code.safeParse("123"))).toBe('incompleteCode {"length":6}')
    expect(code.parse("123456")).toBe("123456")
  })
})
