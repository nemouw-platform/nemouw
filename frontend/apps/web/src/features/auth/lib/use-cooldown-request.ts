"use client"

import { useCountdown } from "@workspace/ui/hooks/use-countdown"
import type { RequestFn } from "@workspace/ui/hooks/use-request"

import {
  isTooManyRequests,
  retryAfterSeconds,
} from "@/features/auth/lib/api-errors"
import { RESEND_COOLDOWN_SECONDS } from "@/features/auth/lib/constants"
import { useAuthRequest } from "@/features/auth/lib/use-auth-request"

interface CooldownOptions<TData> {
  /** Seconds to wait before the first send, e.g. when the previous step just sent one */
  initialCooldown?: number
  onSuccess?: (data: TData) => void
  /** The server says "too soon" (429): the countdown already follows it */
  onThrottled?: () => void
}

/**
 * A request the backend rate-limits per account, such as resending a code or
 * a reset link (60 s between sends). Each success restarts the countdown; a
 * 429 syncs it to the server's Retry-After instead of showing an error.
 */
export function useCooldownRequest<TArgs extends unknown[], TData>(
  request: RequestFn<TArgs, TData>,
  { initialCooldown = 0, onSuccess, onThrottled }: CooldownOptions<TData> = {}
) {
  const countdown = useCountdown(initialCooldown)

  const { run, isPending, formError } = useAuthRequest(request, {
    onSuccess: (data) => {
      countdown.start(RESEND_COOLDOWN_SECONDS)
      onSuccess?.(data)
    },
    onError: (error) => {
      if (!isTooManyRequests(error)) return false
      countdown.start(retryAfterSeconds(error))
      onThrottled?.()
      return true
    },
  })

  return {
    send: run,
    isPending,
    canSend: !countdown.isRunning && !isPending,
    secondsLeft: countdown.remaining,
    formError,
  }
}
