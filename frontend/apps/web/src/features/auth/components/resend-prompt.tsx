import { formatCountdown } from "@workspace/ui/hooks/use-countdown"

import {
  AuthPrompt,
  AuthPromptButton,
} from "@/features/auth/components/auth-prompt"

interface ResendPromptProps {
  /** e.g. "Didn't get the code?" */
  prompt: React.ReactNode
  /** e.g. "Resend code" */
  action: React.ReactNode
  /** e.g. "Sending…" */
  pendingLabel: React.ReactNode
  /** e.g. (time) => `Resend code in ${time}`, shown during the cooldown */
  waitingLabel: (time: string) => React.ReactNode
  onResend: () => void
  pending: boolean
  secondsLeft: number
}

/** "Didn't get it? Resend" with the cooldown countdown in the button label */
export function ResendPrompt({
  prompt,
  action,
  pendingLabel,
  waitingLabel,
  onResend,
  pending,
  secondsLeft,
}: ResendPromptProps) {
  const waiting = secondsLeft > 0

  return (
    <AuthPrompt prompt={prompt}>
      <AuthPromptButton onClick={onResend} disabled={pending || waiting}>
        {pending
          ? pendingLabel
          : waiting
            ? waitingLabel(formatCountdown(secondsLeft))
            : action}
      </AuthPromptButton>
    </AuthPrompt>
  )
}
