"use client"

import { syndicatesApi } from "@/lib/api"
import { useApiLoad } from "@/lib/use-api-load"

/** The public syndicate list for the picker, loaded once, with a retry */
export function useSyndicates() {
  const { data, isLoading, failed, retry } = useApiLoad((signal) =>
    syndicatesApi.list({ signal })
  )
  return { syndicates: data ?? [], isLoading, failed, retry }
}
