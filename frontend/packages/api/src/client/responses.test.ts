import { afterEach, describe, expect, it, vi } from "vitest"

import { parseRetryAfter } from "./responses"

afterEach(() => vi.useRealTimers())

describe("parseRetryAfter", () => {
  it.each([
    [null, null],
    ["", null],
    ["42", 42],
    ["1.2", 2],
    ["-5", 0],
    ["soon", null],
  ])("parses %o as %o", (header, seconds) => {
    expect(parseRetryAfter(header)).toBe(seconds)
  })

  it("parses an HTTP date relative to now", () => {
    vi.useFakeTimers({ now: new Date("2026-11-10T12:00:00Z") })

    expect(parseRetryAfter("Tue, 10 Nov 2026 12:00:30 GMT")).toBe(30)
  })
})
