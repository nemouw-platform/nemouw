import { describe, expect, it } from "vitest"

import { apiError } from "@/features/auth/lib/testing"
import { classifyOnboardingError } from "@/features/auth/onboarding/onboarding-errors"

const fieldError = (field: string, code: "INVALID_FORMAT" | "REQUIRED") =>
  apiError(422, "VALIDATION_ERROR", {
    errors: [{ field, code, message: "bad" }],
  })

describe("classifyOnboardingError", () => {
  it.each([
    [apiError(409, "USERNAME_TAKEN"), "usernameTaken"],
    [apiError(409, "MEMBER_NUMBER_TAKEN"), "memberNumberTaken"],
    [apiError(409, "PROFILE_ALREADY_EXISTS"), "alreadyOnboarded"],
  ] as const)("maps %s to %s", (error, kind) => {
    expect(classifyOnboardingError(error)).toEqual({ kind })
  })

  it("reads a format error on the username as a reserved name", () => {
    expect(
      classifyOnboardingError(fieldError("username", "INVALID_FORMAT"))
    ).toEqual({ kind: "usernameUnavailable" })
  })

  it("keeps other field errors on their fields", () => {
    expect(
      classifyOnboardingError(fieldError("syndicate_member_number", "REQUIRED"))
    ).toEqual({
      kind: "fields",
      fields: [{ name: "syndicate_member_number", code: "REQUIRED" }],
    })
  })

  it.each([apiError(403, "FORBIDDEN"), new Error("offline")])(
    "leaves everything else to the general message (%s)",
    (error) => {
      expect(classifyOnboardingError(error)).toEqual({ kind: "general" })
    }
  )
})
