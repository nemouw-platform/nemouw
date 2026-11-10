import { describe, expect, it } from "vitest"

import {
  containsPersonalInfo,
  hasValidLength,
  passwordLength,
} from "@/features/auth/lib/password-rules"

describe("passwordLength / hasValidLength", () => {
  it("counts code points like the backend (an emoji is one)", () => {
    expect(passwordLength("abc😀")).toBe(4)
  })

  it.each([
    ["1234567", false],
    ["12345678", true],
    ["a".repeat(64), true],
    ["a".repeat(65), false],
  ])("%s -> %s", (password, valid) => {
    expect(hasValidLength(password)).toBe(valid)
  })
})

describe("containsPersonalInfo", () => {
  it.each([
    ["violet-harbor-42", false],
    ["myNemouwGarden", true], // service word, any case
    ["N3m0uw-garden!", true], // leet-decoded
    ["n.e.m.o.u.w-90", true], // letters only
  ])("service word: %s -> %s", (password, contains) => {
    expect(containsPersonalInfo(password)).toBe(contains)
  })

  it("rejects the email's local part (4+ characters) in its forms", () => {
    const email = "rana.haddad@clinic.lb"
    expect(containsPersonalInfo("xx-rana.haddad-xx", email)).toBe(true)
    expect(containsPersonalInfo("ranahaddad2026", email)).toBe(true) // compact form
    expect(containsPersonalInfo("violet-harbor-42", email)).toBe(false)
  })

  it("ignores local parts shorter than 4 characters", () => {
    expect(containsPersonalInfo("bob-loves-tea", "bob@clinic.lb")).toBe(false)
  })

  it("is false for an empty password", () => {
    expect(containsPersonalInfo("", "rana@clinic.lb")).toBe(false)
  })
})
