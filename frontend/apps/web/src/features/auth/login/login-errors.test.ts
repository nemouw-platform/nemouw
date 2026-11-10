import { describe, expect, it } from "vitest"

import { apiError } from "@/features/auth/lib/testing"
import { classifyLoginError } from "@/features/auth/login/login-errors"

describe("classifyLoginError", () => {
  it.each([
    [apiError(401, "INVALID_CREDENTIALS"), "invalidCredentials"],
    [apiError(403, "ACCOUNT_SUSPENDED"), "suspended"],
    [apiError(403, "ACCOUNT_NOT_VERIFIED"), "notVerified"],
  ] as const)("maps %s to %s", (error, kind) => {
    expect(classifyLoginError(error)).toEqual({ kind })
  })

  it("keeps field errors for the form's fields", () => {
    const error = apiError(422, "VALIDATION_ERROR", {
      errors: [{ field: "identifier", code: "REQUIRED", message: "bad" }],
    })

    expect(classifyLoginError(error)).toEqual({
      kind: "fields",
      fields: [{ name: "identifier", code: "REQUIRED" }],
    })
  })

  it.each([apiError(429, "TOO_MANY_REQUESTS", {}, 1), new Error("offline")])(
    "leaves everything else to the general message (%s)",
    (error) => {
      expect(classifyLoginError(error)).toEqual({ kind: "general" })
    }
  )
})
