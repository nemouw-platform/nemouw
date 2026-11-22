"use client"

import { Button } from "@workspace/ui/components/button"
import { LoaderCircleIcon, LogOutIcon } from "lucide-react"
import { useTranslations } from "next-intl"
import * as React from "react"

import { signOut } from "@/features/auth/session/end-session"
import { authApi } from "@/lib/api"

/** Logs out and opens the login page (a full page load) */
export function SignOutButton({
  className,
  size = "default",
}: {
  className?: string
  size?: "default" | "lg"
}) {
  const t = useTranslations("Common")
  const [pending, setPending] = React.useState(false)

  return (
    <Button
      variant="ghost"
      size={size}
      className={className}
      disabled={pending}
      onClick={() => {
        setPending(true)
        signOut(() => authApi.logout())
      }}
    >
      {pending ? (
        <LoaderCircleIcon className="animate-spin" data-icon="inline-start" />
      ) : (
        <LogOutIcon data-icon="inline-start" className="rtl:-scale-x-100" />
      )}
      {t("signOut")}
    </Button>
  )
}
