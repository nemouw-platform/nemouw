import { CLIENT_ERROR_CODES } from "@workspace/api"
import { describe, expect, it, vi } from "vitest"

import {
  generalErrorMessage,
  pickFieldErrors,
  retryAfterSeconds,
  type AuthErrorsTranslator,
} from "@/features/auth/lib/api-errors"
import { apiError } from "@/features/auth/lib/testing"

/** Translator stub that echoes the key and values */
const t = vi.fn((key: string, values?: object) =>
  values ? `${key} ${JSON.stringify(values)}` : key
) as unknown as AuthErrorsTranslator

describe("pickFieldErrors", () => {
  const error = apiError(422, "VALIDATION_ERROR", {
    errors: [
      { field: "email", code: "MUST_BE_EMAIL", message: "bad" },
      { field: "role", code: "INVALID_ROLE", message: "bad" },
    ],
  })

  it("keeps only the form's fields", () => {
    expect(pickFieldErrors(error, ["email", "first_name"])).toEqual([
      { name: "email", code: "MUST_BE_EMAIL" },
    ])
  })

  it("is empty for anything but a validation error", () => {
    expect(pickFieldErrors(apiError(409, "EMAIL_TAKEN"), ["email"])).toEqual([])
    expect(pickFieldErrors(new Error("x"), ["email"])).toEqual([])
  })
})

describe("retryAfterSeconds", () => {
  it("uses Retry-After, else the 60 s cooldown", () => {
    expect(retryAfterSeconds(apiError(429, "TOO_MANY_REQUESTS", {}, 7))).toBe(7)
    expect(retryAfterSeconds(apiError(429, "TOO_MANY_REQUESTS"))).toBe(60)
  })
})

describe("generalErrorMessage", () => {
  it.each([
    [
      apiError(429, "TOO_MANY_REQUESTS", {}, 5),
      'tooManyRequests {"seconds":5}',
    ],
    [apiError(0, CLIENT_ERROR_CODES.network), "network"],
    [apiError(500, "INTERNAL_ERROR"), "unexpected"],
    [new Error("boom"), "unexpected"],
  ])("maps %s to %s", (error, message) => {
    expect(generalErrorMessage(error, t)).toBe(message)
  })
})
