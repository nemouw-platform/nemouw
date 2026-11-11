import { describe, expect, it } from "vitest"

import type { AuthErrorsTranslator } from "@/features/auth/lib/api-errors"
import { createOnboardingSchema } from "@/features/auth/onboarding/onboarding-schema"

/** Translator stub that echoes the key and values */
const t = ((key: string, values?: object) =>
  values ? `${key} ${JSON.stringify(values)}` : key) as AuthErrorsTranslator

const schema = createOnboardingSchema(
  { username: "Username", syndicate: "Syndicate", memberNumber: "Member number" },
  t,
  "usernameFormat"
)

/** The first message per field, as the form shows it */
const errorsOf = (values: object) => {
  const result = schema.safeParse(values)
  const errors: Record<string, string> = {}
  for (const issue of result.error?.issues ?? []) {
    errors[String(issue.path[0])] ??= issue.message
  }
  return errors
}

describe("createOnboardingSchema", () => {
  it("reports every empty field at once", () => {
    expect(
      errorsOf({ username: "", syndicate_id: "", syndicate_member_number: "" })
    ).toEqual({
      username: 'required {"field":"Username"}',
      syndicate_id: 'required {"field":"Syndicate"}',
      syndicate_member_number: 'required {"field":"Member number"}',
    })
  })

  it.each([
    ["ab", 'tooShort {"min":3}'],
    ["no spaces!", "usernameFormat"],
    ["a".repeat(31), 'tooLong {"max":30}'],
  ])("username %s -> %s", (username, message) => {
    expect(
      errorsOf({ username, syndicate_id: "s", syndicate_member_number: "1" })
        .username
    ).toBe(message)
  })

  it("accepts a valid profile", () => {
    expect(
      errorsOf({
        username: "dr.rana-haddad",
        syndicate_id: "s",
        syndicate_member_number: "12345",
      })
    ).toEqual({})
  })
})
