"use client"

import type { ApiLocale, SpecialistProfileResponse } from "@workspace/api"
import * as React from "react"

import { specialistsApi } from "@/lib/api"
import { useApiLoad } from "@/lib/use-api-load"

export type ContentByLocale = Partial<
  Record<ApiLocale, SpecialistProfileResponse>
>

function byLocale(rows: SpecialistProfileResponse[]): ContentByLocale {
  return Object.fromEntries(rows.map((row) => [row.locale_code, row]))
}

/**
 * The specialist's public text in each language, loaded once. `replace` swaps
 * in what a save returned.
 */
export function useProfileContent() {
  const [content, setContent] = React.useState<ContentByLocale>({})
  const { isLoading, failed, error, retry } = useApiLoad(
    (signal) => specialistsApi.listContent({ signal }),
    { onSuccess: (rows) => setContent(byLocale(rows)) }
  )

  const replace = React.useCallback((row: SpecialistProfileResponse) => {
    if (!row.locale_code) return
    setContent((current) => ({ ...current, [row.locale_code!]: row }))
  }, [])

  return { content, isLoading, failed, error, retry, replace }
}
