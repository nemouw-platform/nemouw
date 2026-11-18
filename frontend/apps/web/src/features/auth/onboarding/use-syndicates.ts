"use client"

import { useLoad } from "@workspace/ui/hooks/use-load"

import { syndicatesApi } from "@/lib/api"

/** The public syndicate list for the picker, loaded once, with a retry */
export function useSyndicates() {
  const { data, isLoading, failed, retry } = useLoad((signal) =>
    syndicatesApi.list({ signal })
  )
  return { syndicates: data ?? [], isLoading, failed, retry }
}
