import { describe, expect, it } from "vitest"

import {
  countryOptions,
  matchesCountry,
} from "@/components/form/phone-input/countries"
import {
  examplePhone,
  formatPhone,
  isPhoneNumber,
  joinPhone,
  readTyped,
  splitPhone,
} from "@/components/form/phone-input/phone"

describe("splitPhone", () => {
  it("places a saved number in its country, formatted nationally", () => {
    expect(splitPhone("+96170123456", "FR")).toEqual({
      country: "LB",
      national: "70 123 456",
    })
  })

  it("starts an empty value on the fallback country", () => {
    expect(splitPhone("", "AE")).toEqual({ country: "AE", national: "" })
  })
})

describe("joinPhone", () => {
  it("builds the international number, dropping a national 0", () => {
    expect(joinPhone("70 123 456", "LB")).toBe("+96170123456")
    expect(joinPhone("06 12 34 56 78", "FR")).toBe("+33612345678")
  })

  it("keeps an incomplete number so validation can flag it", () => {
    expect(joinPhone("7", "LB")).toBe("+9617")
    expect(isPhoneNumber(joinPhone("7", "LB"))).toBe(false)
  })

  it("is empty when no digit is typed", () => {
    expect(joinPhone("", "LB")).toBe("")
    expect(joinPhone(" - ", "LB")).toBe("")
  })
})

describe("readTyped", () => {
  it("lets a number with + pick its own country", () => {
    expect(readTyped("+33 6 12 34 56 78", "LB")).toEqual({
      country: "FR",
      national: "06 12 34 56 78",
    })
  })

  it("keeps national input on the picked country", () => {
    expect(readTyped("70 12", "LB")).toEqual({
      country: "LB",
      national: "70 12",
    })
  })
})

describe("formatPhone", () => {
  it("writes a saved number with spaces, leaving others as they are", () => {
    expect(formatPhone("+96170123456")).toBe("+961 70 123 456")
    expect(formatPhone("call me")).toBe("call me")
  })
})

describe("isPhoneNumber / examplePhone", () => {
  it("accepts a valid number and the country's own example", () => {
    expect(isPhoneNumber("+96170123456")).toBe(true)
    expect(isPhoneNumber("+961123")).toBe(false)
    expect(isPhoneNumber(joinPhone(examplePhone("LB"), "LB"))).toBe(true)
  })
})

describe("countryOptions / matchesCountry", () => {
  const lebanon = (locale: string) =>
    countryOptions(locale).find((option) => option.code === "LB")

  it("names countries in the page's language", () => {
    expect(lebanon("en")).toEqual({
      code: "LB",
      name: "Lebanon",
      callingCode: "961",
    })
    expect(lebanon("fr")?.name).toBe("Liban")
  })

  it("finds a country by name, code or dialing code", () => {
    const option = lebanon("en")!
    for (const query of ["leb", "LB", "961", "+961", ""]) {
      expect(matchesCountry(option, query)).toBe(true)
    }
    expect(matchesCountry(option, "france")).toBe(false)
    expect(matchesCountry(option, "lbn")).toBe(false)
  })

  it("ignores accents", () => {
    const ivoryCoast = countryOptions("fr").find((o) => o.code === "CI")!
    expect(matchesCountry(ivoryCoast, "cote")).toBe(true)
  })
})
