import { cn } from "@workspace/ui/lib/utils"
import { CheckIcon, CircleIcon, InfoIcon } from "lucide-react"
import { useTranslations } from "next-intl"

import {
  containsPersonalInfo,
  hasValidLength,
} from "@/features/auth/lib/password-rules"

type RuleState = "met" | "unmet" | "server"

/**
 * The password rules as a checklist that ticks off while the user types.
 * The common-password rule is only known to the server, so it stays informative.
 */
export function PasswordRequirements({
  password,
  email,
}: {
  password: string
  email?: string
}) {
  const t = useTranslations("Auth.password")
  const typed = password.length > 0

  const rules: Array<{ label: string; state: RuleState }> = [
    { label: t("length"), state: hasValidLength(password) ? "met" : "unmet" },
    {
      label: t("personal"),
      state: typed && !containsPersonalInfo(password, email) ? "met" : "unmet",
    },
    { label: t("common"), state: "server" },
  ]

  return (
    <ul aria-label={t("requirements")} className="flex flex-col gap-1">
      {rules.map(({ label, state }) => (
        <li
          key={label}
          className={cn(
            "flex items-start gap-2",
            state === "met" && "text-foreground"
          )}
        >
          <RuleIcon state={state} />
          <span>
            {label}
            {state !== "server" && (
              <span className="sr-only">
                {" "}
                ({state === "met" ? t("met") : t("notMet")})
              </span>
            )}
          </span>
        </li>
      ))}
    </ul>
  )
}

function RuleIcon({ state }: { state: RuleState }) {
  const className = "mt-0.5 size-4 shrink-0"
  if (state === "met") {
    return <CheckIcon className={cn(className, "text-primary")} aria-hidden />
  }
  if (state === "server") {
    return <InfoIcon className={className} aria-hidden />
  }
  return <CircleIcon className={cn(className, "scale-50")} aria-hidden />
}
