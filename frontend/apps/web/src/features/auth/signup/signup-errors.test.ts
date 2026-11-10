import { ApiError, CLIENT_ERROR_CODES } from "@workspace/api"
import { describe, expect, it } from "vitest"

import { classifySignupError } from "@/features/auth/signup/signup-errors"

const apiError = (
  status: number,
  code: string,
  extra: Partial<ConstructorParameters<typeof ApiError>[0]> = {},
  retryAfter: number | null = null
) => new ApiError({ status, code, message: code, ...extra }, retryAfter)

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

  it("falls back to the 60 s cooldown when Retry-After is missing", () => {
    expect(classifySignupError(apiError(429, "TOO_MANY_REQUESTS"))).toEqual({
      kind: "codeAlreadySent",
      retryAfterSeconds: 60,
    })
  })

  it("keeps field errors for form fields and drops the rest", () => {
    const error = apiError(422, "VALIDATION_ERROR", {
      errors: [
        { field: "email", code: "MUST_BE_EMAIL", message: "bad" },
        { field: "role", code: "INVALID_ROLE", message: "bad" },
      ],
    })

    expect(classifySignupError(error)).toEqual({
      kind: "fields",
      fields: [{ name: "email", code: "MUST_BE_EMAIL" }],
    })
  })

  it("treats a 422 with no form field errors as general", () => {
    const error = apiError(422, "VALIDATION_ERROR", {
      errors: [{ field: "role", code: "INVALID_ROLE", message: "bad" }],
    })

    expect(classifySignupError(error)).toEqual({ kind: "general", error })
  })

  it.each([
    apiError(0, CLIENT_ERROR_CODES.network),
    apiError(500, "INTERNAL_ERROR"),
    new Error("not an ApiError"),
  ])("treats anything else as general (%s)", (error) => {
    expect(classifySignupError(error)).toEqual({ kind: "general", error })
  })
})
