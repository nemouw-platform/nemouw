import type { CredentialResponse } from "@workspace/api"
import { describe, expect, it } from "vitest"

import {
  groupBySpecialist,
  matchesFilter,
} from "@/features/syndicate/review/review-queue"
import { findMember } from "@/features/syndicate/me/membership"

const credential = (
  specialist: string,
  status: CredentialResponse["verification_status"],
  updated: string
): CredentialResponse => ({
  id: `${specialist}-${updated}`,
  specialist_id: specialist,
  verification_status: status,
  updated_at: updated,
})

describe("groupBySpecialist", () => {
  it("gathers credentials per specialist, most waiting first, then latest", () => {
    const entries = groupBySpecialist([
      credential("a", "verified", "2026-11-01"),
      credential("b", "pending", "2026-11-02"),
      credential("c", "pending", "2026-11-05"),
      credential("b", "pending", "2026-11-03"),
      credential("a", "rejected", "2026-11-09"),
    ])
    expect(entries.map((e) => [e.specialistId, e.pending])).toEqual([
      ["b", 2],
      ["c", 1],
      ["a", 0],
    ])
    expect(entries[2]!.credentials).toHaveLength(2)
  })
})

describe("matchesFilter", () => {
  const waiting = { specialistId: "a", credentials: [], pending: 1 }
  const done = { specialistId: "a", credentials: [], pending: 0 }

  it("puts undecided specialists and waiting credentials under To review", () => {
    expect(
      matchesFilter("toReview", done, { verification_status: "pending" })
    ).toBe(true)
    expect(
      matchesFilter("toReview", waiting, { verification_status: "active" })
    ).toBe(true)
    expect(
      matchesFilter("toReview", done, { verification_status: "active" })
    ).toBe(false)
  })

  it("files decided specialists by their status", () => {
    expect(
      matchesFilter("verified", done, { verification_status: "active" })
    ).toBe(true)
    expect(
      matchesFilter("rejected", done, { verification_status: "rejected" })
    ).toBe(true)
    expect(matchesFilter("all", done, undefined)).toBe(true)
  })
})

describe("findMember", () => {
  it("finds the user's own row in a staff list", () => {
    const staff = [
      { id: "s1", user: { id: "u1" } },
      { id: "s2", user: { id: "u2" }, is_owner: true },
    ]
    expect(findMember(staff, "u2")?.id).toBe("s2")
    expect(findMember(staff, "u9")).toBeUndefined()
  })
})
