import { describe, expect, it } from "vitest"

import { apiError } from "@/features/auth/lib/testing"
import { classifyResetError } from "@/features/auth/reset/reset-errors"

describe("classifyResetError", () => {
  it("treats the backend's 400 as an invalid link", () => {
    expect(
      classifyResetError(apiError(400, "INVALID_OR_EXPIRED_TOKEN"))
    ).toEqual({ kind: "invalidLink" })
  })

  it("treats a 422 on `token` as an invalid link", () => {
    const error = apiError(422, "VALIDATION_ERROR", {
      errors: [{ field: "token", code: "REQUIRED", message: "bad" }],
    })
    expect(classifyResetError(error)).toEqual({ kind: "invalidLink" })
  })

  it("puts password rules from the server on the field", () => {
    const error = apiError(422, "VALIDATION_ERROR", {
      errors: [{ field: "new_password", code: "TOO_COMMON", message: "bad" }],
    })
    expect(classifyResetError(error)).toEqual({
      kind: "fields",
      fields: [{ name: "new_password", code: "TOO_COMMON" }],
    })
  })

  it.each([apiError(429, "TOO_MANY_REQUESTS", {}, 1), new Error("offline")])(
    "leaves everything else to the general message (%s)",
    (error) => {
      expect(classifyResetError(error)).toEqual({ kind: "general" })
    }
  )
})
