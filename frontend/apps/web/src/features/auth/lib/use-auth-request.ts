"use client"

import { useRequest, type RequestFn } from "@workspace/ui/hooks/use-request"
import { useTranslations } from "next-intl"
import * as React from "react"

import { generalErrorMessage } from "@/features/auth/lib/api-errors"

interface AuthRequestOptions<TData> {
  onSuccess?: (data: TData) => void
  /**
   * Handle the errors this screen understands (put them on a field, show a
   * specific message) and return true. Anything left shows the general message.
   */
  onError?: (error: unknown) => boolean
}

/**
 * useRequest plus the form-level message every auth screen needs: cleared on
 * each run, set for errors the screen does not handle itself.
 */
export function useAuthRequest<TArgs extends unknown[], TData>(
  request: RequestFn<TArgs, TData>,
  { onSuccess, onError }: AuthRequestOptions<TData> = {}
) {
  const t = useTranslations("Auth.errors")
  const [formError, setFormError] = React.useState<string | null>(null)

  const { run: runRequest, isPending } = useRequest(request, {
    onSuccess,
    onError: (error) => {
      if (!onError?.(error)) setFormError(generalErrorMessage(error, t))
    },
  })

  const run = React.useCallback(
    (...args: TArgs) => {
      setFormError(null)
      runRequest(...args)
    },
    [runRequest]
  )

  return { run, isPending, formError, setFormError }
}
