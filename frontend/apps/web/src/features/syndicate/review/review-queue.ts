import type { CredentialResponse, SpecialistResponse } from "@workspace/api"

/** One specialist in the review queue, with their credentials */
export interface QueueEntry {
  specialistId: string
  credentials: CredentialResponse[]
  /** Credentials waiting for a decision */
  pending: number
}

/**
 * The syndicate's credentials, gathered per specialist: those with the most
 * credentials waiting first, then the most recently changed
 */
export function groupBySpecialist(
  credentials: CredentialResponse[]
): QueueEntry[] {
  const bySpecialist = new Map<string, CredentialResponse[]>()
  for (const credential of credentials) {
    if (!credential.specialist_id) continue
    const list = bySpecialist.get(credential.specialist_id) ?? []
    list.push(credential)
    bySpecialist.set(credential.specialist_id, list)
  }
  const latest = (list: CredentialResponse[]) =>
    list.reduce(
      (most, c) => ((c.updated_at ?? "") > most ? (c.updated_at ?? "") : most),
      ""
    )

  return [...bySpecialist.entries()]
    .map(([specialistId, list]) => ({
      specialistId,
      credentials: list,
      pending: list.filter((c) => c.verification_status === "pending").length,
    }))
    .sort(
      (a, b) =>
        b.pending - a.pending ||
        latest(b.credentials).localeCompare(latest(a.credentials))
    )
}

/** The queue's tabs */
export type QueueFilter = "toReview" | "verified" | "rejected" | "all"

/**
 * Whether a specialist belongs under a tab. "To review": the specialist is
 * not decided yet, or has credentials waiting.
 */
export function matchesFilter(
  filter: QueueFilter,
  entry: QueueEntry,
  specialist: SpecialistResponse | undefined
): boolean {
  const status = specialist?.verification_status ?? "pending"
  switch (filter) {
    case "toReview":
      return status === "pending" || entry.pending > 0
    case "verified":
      return status === "active"
    case "rejected":
      return status === "rejected"
    case "all":
      return true
  }
}
