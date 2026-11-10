import { describe, expect, it } from "vitest"

import { apiError } from "@/features/auth/lib/testing"
import { classifyVerifyError } from "@/features/auth/verify/verify-errors"

describe("classifyVerifyError", () => {
  it("treats the backend's single 400 as a rejected code", () => {
    expect(classifyVerifyError(apiError(400, "INVALID_OR_EXPIRED_TOKEN"))).toBe(
      "invalidCode"
    )
  })

  it("treats a 422 on `code` as a rejected code", () => {
    const error = apiError(422, "VALIDATION_ERROR", {
      errors: [{ field: "code", code: "INVALID_FORMAT", message: "bad" }],
    })

    expect(classifyVerifyError(error)).toBe("invalidCode")
  })

  it.each([
    apiError(429, "TOO_MANY_REQUESTS", {}, 3),
    apiError(422, "VALIDATION_ERROR", {
      errors: [{ field: "email", code: "MUST_BE_EMAIL", message: "bad" }],
    }),
    new Error("offline"),
  ])("leaves everything else to the general message (%s)", (error) => {
    expect(classifyVerifyError(error)).toBe("general")
  })
})
