import { describe, expect, it } from "vitest"

import { apiError } from "@/features/auth/lib/testing"
import { classifyPasswordError } from "@/features/auth/password/password-errors"

describe("classifyPasswordError", () => {
  it.each([
    apiError(401, "UNAUTHENTICATED"),
    apiError(401, "INVALID_TOKEN"),
    apiError(403, "FORBIDDEN"),
  ])("treats %s as an expired setup step", (error) => {
    expect(classifyPasswordError(error)).toEqual({ kind: "setupExpired" })
  })

  it("puts server password rules on the field", () => {
    const error = apiError(422, "VALIDATION_ERROR", {
      errors: [{ field: "password", code: "TOO_COMMON", message: "bad" }],
    })

    expect(classifyPasswordError(error)).toEqual({
      kind: "fields",
      fields: [{ name: "password", code: "TOO_COMMON" }],
    })
  })

  it.each([apiError(429, "TOO_MANY_REQUESTS", {}, 1), new Error("offline")])(
    "leaves everything else to the general message (%s)",
    (error) => {
      expect(classifyPasswordError(error)).toEqual({ kind: "general" })
    }
  )
})
