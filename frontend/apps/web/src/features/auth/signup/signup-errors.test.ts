import { CLIENT_ERROR_CODES } from "@workspace/api"
import { describe, expect, it } from "vitest"

import { apiError } from "@/features/auth/lib/testing"
import { classifySignupError } from "@/features/auth/signup/signup-errors"

describe("classifySignupError", () => {
  it("maps 409 EMAIL_TAKEN to the email field case", () => {
    expect(classifySignupError(apiError(409, "EMAIL_TAKEN"))).toEqual({
      kind: "emailTaken",
    })
  })

  it("maps 429 to 'code already sent' with the Retry-After seconds", () => {
    expect(
      classifySignupError(apiError(429, "TOO_MANY_REQUESTS", {}, 42))
    ).toEqual({ kind: "codeAlreadySent", retryAfterSeconds: 42 })
  })

  it("keeps field errors for form fields", () => {
    const error = apiError(422, "VALIDATION_ERROR", {
      errors: [{ field: "email", code: "MUST_BE_EMAIL", message: "bad" }],
    })

    expect(classifySignupError(error)).toEqual({
      kind: "fields",
      fields: [{ name: "email", code: "MUST_BE_EMAIL" }],
    })
  })

  it.each([
    apiError(422, "VALIDATION_ERROR", {
      errors: [{ field: "role", code: "INVALID_ROLE", message: "bad" }],
    }),
    apiError(0, CLIENT_ERROR_CODES.network),
    apiError(500, "INTERNAL_ERROR"),
    new Error("not an ApiError"),
  ])("leaves everything else to the general message (%s)", (error) => {
    expect(classifySignupError(error)).toEqual({ kind: "general" })
  })
})
