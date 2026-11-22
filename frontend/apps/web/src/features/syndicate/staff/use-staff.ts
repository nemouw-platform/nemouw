"use client"

import type { SyndicateStaffResponse } from "@workspace/api"
import { useLoad } from "@workspace/ui/hooks/use-load"
import * as React from "react"

import { syndicatesApi } from "@/lib/api"

/** The owner first, then everyone else as they joined */
function byOwnerThenJoined(
  a: SyndicateStaffResponse,
  b: SyndicateStaffResponse
): number {
  if (a.is_owner !== b.is_owner) return a.is_owner ? -1 : 1
  return (a.created_at ?? "").localeCompare(b.created_at ?? "")
}

/**
 * The syndicate's staff, kept in step with what the owner changes here
 * (invites, titles, removals) without loading the list again
 */
export function useStaff(syndicateId: string) {
  const [staff, setStaff] = React.useState<SyndicateStaffResponse[] | null>(
    null
  )
  const load = useLoad(
    (signal) => syndicatesApi.listStaff(syndicateId, { signal }),
    { onSuccess: (list) => setStaff([...list].sort(byOwnerThenJoined)) }
  )

  const added = React.useCallback(
    (member: SyndicateStaffResponse) =>
      setStaff((current) => current && [...current, member]),
    []
  )
  const replaced = React.useCallback(
    (member: SyndicateStaffResponse) =>
      setStaff(
        (current) =>
          current && current.map((m) => (m.id === member.id ? member : m))
      ),
    []
  )
  const removed = React.useCallback(
    (id: string) =>
      setStaff((current) => current && current.filter((m) => m.id !== id)),
    []
  )

  return { ...load, staff, added, replaced, removed }
}
