import { describe, expect, it } from "vitest"

import {
  countryFromHeaders,
  DEFAULT_COUNTRY,
} from "@/lib/visitor-country/country-from-headers"

describe("countryFromHeaders", () => {
  it("reads the host's geo-IP header", () => {
    expect(
      countryFromHeaders(new Headers({ "x-vercel-ip-country": "fr" }))
    ).toBe("FR")
    expect(countryFromHeaders(new Headers({ "cf-ipcountry": "AE" }))).toBe("AE")
  })

  it("prefers the first header present", () => {
    const headers = new Headers({
      "x-vercel-ip-country": "SA",
      "cf-ipcountry": "AE",
    })
    expect(countryFromHeaders(headers)).toBe("SA")
  })

  it("falls back to Lebanon without a usable header", () => {
    expect(countryFromHeaders(new Headers())).toBe(DEFAULT_COUNTRY)
    expect(countryFromHeaders(new Headers({ "cf-ipcountry": "XX" }))).toBe("LB")
    expect(countryFromHeaders(new Headers({ "cf-ipcountry": "T1" }))).toBe("LB")
  })
})
