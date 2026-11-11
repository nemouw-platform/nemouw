"use client"

import { useLocale } from "next-intl"
import * as React from "react"

import { RESEND_COOLDOWN_SECONDS } from "@/features/auth/lib/constants"
import { useCooldownRequest } from "@/features/auth/lib/use-cooldown-request"
import { authApi } from "@/lib/api"

/** "Resend code". The countdown starts on arrival: step 1 just sent a code. */
export function useResendCode(email: string) {
  const locale = useLocale()
  const [resent, setResent] = React.useState(false)

  const resend = useCooldownRequest(
    (signal) => authApi.resendOtp({ email, locale }, { signal }),
    {
      initialCooldown: RESEND_COOLDOWN_SECONDS,
      onSuccess: () => setResent(true),
    }
  )

  return {
    resend: () => {
      setResent(false)
      resend.send()
    },
    isPending: resend.isPending,
    secondsLeft: resend.secondsLeft,
    resent,
    formError: resend.formError,
  }
}
