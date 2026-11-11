"use client"

import { useRequest } from "@workspace/ui/hooks/use-request"
import * as React from "react"

import { syndicatesApi } from "@/lib/api"

/** The public syndicate list for the picker, loaded once, with a retry */
export function useSyndicates() {
  const { data, status, run, restart } = useRequest(
    (signal) => syndicatesApi.list({ signal }),
    { debounceMs: 0 }
  )

  React.useEffect(() => run(), [run])

  return {
    syndicates: data ?? [],
    isLoading: status === "idle" || status === "pending",
    failed: status === "error",
    retry: restart,
  }
}
