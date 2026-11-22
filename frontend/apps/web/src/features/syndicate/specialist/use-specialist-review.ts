"use client"

import type { CredentialResponse, SpecialistResponse } from "@workspace/api"
import { useLoad } from "@workspace/ui/hooks/use-load"
import * as React from "react"

import { reviewsApi, syndicatesApi } from "@/lib/api"

interface SpecialistReview {
  specialist: SpecialistResponse
  credentials: CredentialResponse[]
}

/**
 * One specialist under review: their profile and the credentials they sent
 * the syndicate. Decisions swap in what the backend returns, so the page
 * shows the new status without loading again.
 */
export function useSpecialistReview(syndicateId: string, specialistId: string) {
  const [review, setReview] = React.useState<SpecialistReview | null>(null)
  const load = useLoad(
    async (signal): Promise<SpecialistReview> => {
      // The backend has no "credentials of one specialist" for staff: take
      // them from the syndicate's queue
      const [specialist, queue] = await Promise.all([
        reviewsApi.getSpecialist(specialistId, { signal }),
        syndicatesApi.credentialQueue(syndicateId, undefined, { signal }),
      ])
      return {
        specialist,
        credentials: queue.filter((c) => c.specialist_id === specialistId),
      }
    },
    { onSuccess: setReview }
  )

  const setSpecialist = React.useCallback(
    (specialist: SpecialistResponse) =>
      setReview((current) => current && { ...current, specialist }),
    []
  )
  const setCredential = React.useCallback(
    (credential: CredentialResponse) =>
      setReview(
        (current) =>
          current && {
            ...current,
            credentials: current.credentials.map((c) =>
              c.id === credential.id ? credential : c
            ),
          }
      ),
    []
  )

  return { ...load, review, setSpecialist, setCredential }
}
