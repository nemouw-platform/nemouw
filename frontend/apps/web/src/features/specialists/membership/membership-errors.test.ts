import { classifyMembershipError } from "@/features/specialists/membership/membership-errors"
import { MEMBERSHIP_FIELDS } from "@/features/specialists/membership/membership-schema"
import { describe, expect, it } from "vitest"

import { apiError } from "@/lib/forms/testing"

const fieldError = (field: string, code: "INVALID_FORMAT" | "REQUIRED") =>
  apiError(422, "VALIDATION_ERROR", {
    errors: [{ field, code, message: "bad" }],
  })

const classify = (error: unknown) =>
  classifyMembershipError(error, MEMBERSHIP_FIELDS)

describe("classifyMembershipError", () => {
  it.each([
    [apiError(409, "USERNAME_TAKEN"), "usernameTaken"],
    [apiError(409, "MEMBER_NUMBER_TAKEN"), "memberNumberTaken"],
    [apiError(409, "SYNDICATE_LOCKED"), "syndicateLocked"],
    [apiError(409, "PROFILE_ALREADY_EXISTS"), "alreadyOnboarded"],
  ] as const)("maps %s to %s", (error, kind) => {
    expect(classify(error)).toEqual({ kind })
  })

  it("reads a format error on the username as a reserved name", () => {
    expect(classify(fieldError("username", "INVALID_FORMAT"))).toEqual({
      kind: "usernameUnavailable",
    })
  })

  it("keeps other field errors on their fields", () => {
    expect(classify(fieldError("syndicate_member_number", "REQUIRED"))).toEqual(
      {
        kind: "fields",
        fields: [{ name: "syndicate_member_number", code: "REQUIRED" }],
      }
    )
  })

  it.each([apiError(403, "FORBIDDEN"), new Error("offline")])(
    "leaves everything else to the general message (%s)",
    (error) => {
      expect(classify(error)).toEqual({ kind: "general" })
    }
  )
})

describe("classifyMembershipError with some fields", () => {
  it("leaves out field errors for fields the form does not have", () => {
    const error = apiError(422, "VALIDATION_ERROR", {
      errors: [
        { field: "syndicate_member_number", code: "REQUIRED", message: "x" },
      ],
    })
    expect(classifyMembershipError(error, ["username"])).toEqual({
      kind: "general",
    })
  })
})
