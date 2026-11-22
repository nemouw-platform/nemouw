"use client"

import type { SpecialistResponse } from "@workspace/api"
import { useLoad } from "@workspace/ui/hooks/use-load"

import { groupBySpecialist } from "@/features/syndicate/review/review-queue"
import { reviewsApi, syndicatesApi } from "@/lib/api"
import { mapWithLimit } from "@/lib/concurrency"

/** Profiles fetched at once: the backend allows about 5 requests a second */
const PROFILE_CONCURRENCY = 3

/**
 * The syndicate's review queue: every credential, gathered per specialist,
 * with each specialist's profile.
 *
 * The backend has no list of a syndicate's specialists for staff yet, so
 * specialists come from their credentials (one profile request each), and a
 * specialist who has not uploaded any credential does not show.
 */
export function useReviewQueue(syndicateId: string) {
  return useLoad(async (signal) => {
    const credentials = await syndicatesApi.credentialQueue(
      syndicateId,
      undefined,
      { signal }
    )
    const entries = groupBySpecialist(credentials)
    const profiles = await mapWithLimit(entries, PROFILE_CONCURRENCY, (entry) =>
      reviewsApi.getSpecialist(entry.specialistId, { signal })
    )
    const specialists = new Map<string, SpecialistResponse>(
      entries.map((entry, index) => [entry.specialistId, profiles[index]!])
    )
    return { entries, specialists }
  })
}
