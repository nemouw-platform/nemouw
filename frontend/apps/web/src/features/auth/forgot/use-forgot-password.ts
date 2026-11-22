"use client"

import { useLocale } from "next-intl"
import * as React from "react"
import type { UseFormReturn } from "react-hook-form"

import type { ForgotValues } from "@/features/auth/forgot/forgot-schema"
import { useCooldownRequest } from "@/features/auth/lib/use-cooldown-request"
import { authApi } from "@/lib/api"

/**
 * Requests the reset email. The backend answers 202 whether or not the
 * account exists (so it reveals nothing), and 429 when a link went out under
 * a minute ago: both mean "check your inbox".
 */
export function useForgotPassword(form: UseFormReturn<ForgotValues>) {
  const locale = useLocale()
  const [sentTo, setSentTo] = React.useState<string | null>(null)
  const [resent, setResent] = React.useState(false)

  const request = useCooldownRequest(
    (signal, email: string) =>
      authApi.forgotPassword({ email, locale }, { signal }),
    {
      onSuccess: () => {
        if (sentTo) setResent(true)
        setSentTo(form.getValues("email"))
      },
      onThrottled: () => setSentTo(form.getValues("email")),
    }
  )

  return {
    submit: form.handleSubmit(({ email }) => request.send(email)),
    resend: () => {
      setResent(false)
      if (sentTo) request.send(sentTo)
    },
    isPending: request.isPending,
    secondsLeft: request.secondsLeft,
    formError: request.formError,
    sentTo,
    resent,
  }
}
