"use client"

import { useLoad } from "@workspace/ui/hooks/use-load"
import * as React from "react"

import { conditionsApi, specialistsApi } from "@/lib/api"

/** The ids of the conditions the specialist treats, loaded once */
export function useTreatedConditions() {
  const [ids, setIds] = React.useState<string[]>([])
  const load = useLoad((signal) => specialistsApi.listConditions({ signal }), {
    onSuccess: setIds,
  })
  return { ids, setIds, ...load }
}

/** Every condition specialists can pick, loaded once */
export function useConditionList() {
  const load = useLoad((signal) => conditionsApi.list(true, { signal }))
  return { conditions: load.data ?? [], ...load }
}
