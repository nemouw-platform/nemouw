"use client"

import { useRequest, type RequestFn } from "@workspace/ui/hooks/use-request"
import { useTranslations } from "next-intl"
import * as React from "react"

import { generalErrorMessage } from "@/lib/forms/api-errors"

interface FormRequestOptions<TData> {
  onSuccess?: (data: TData) => void
  /**
   * Handle the errors this screen understands (put them on a field, show a
   * specific message) and return true. Anything left shows the general message.
   */
  onError?: (error: unknown) => boolean
}

/**
 * useRequest plus the form-level message every form needs: cleared on
 * each run, set for errors the screen does not handle itself.
 */
export function useFormRequest<TArgs extends unknown[], TData>(
  request: RequestFn<TArgs, TData>,
  { onSuccess, onError }: FormRequestOptions<TData> = {}
) {
  const t = useTranslations("Form.errors")
  const [formError, setFormError] = React.useState<string | null>(null)

  const {
    run: runRequest,
    isPending,
    status,
  } = useRequest(request, {
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

  /** The latest request succeeded (and none is running), e.g. to say "Saved" */
  const succeeded = status === "success" && !isPending

  return { run, isPending, succeeded, formError, setFormError }
}
