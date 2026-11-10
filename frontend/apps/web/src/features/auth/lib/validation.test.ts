import { describe, expect, it } from "vitest"

import type { AuthErrorsTranslator } from "@/features/auth/lib/api-errors"
import { createFieldRules } from "@/features/auth/lib/validation"

/** Translator stub that echoes the key and values */
const t = ((key: string, values?: object) =>
  values ? `${key} ${JSON.stringify(values)}` : key) as AuthErrorsTranslator

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
