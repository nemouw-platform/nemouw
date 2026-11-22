import { buttonVariants } from "@workspace/ui/components/button"
import { cn } from "@workspace/ui/lib/utils"
import { LayoutDashboardIcon } from "lucide-react"
import { useTranslations } from "next-intl"
import { getTranslations } from "next-intl/server"
import { Suspense } from "react"

import { SignOutButton } from "@/features/auth/components/sign-out-button"
import { homePathFor } from "@/features/auth/lib/constants"
import { getCookieSession } from "@/features/auth/session/server-session"
import { Link } from "@/i18n/navigation"

interface AccountLinksProps {
  className?: string
  size?: "default" | "lg"
}

/**
 * The account actions at the end of the navbar, for whoever is visiting:
 * log in and sign up, or (signed in) log out, plus the dashboard for a
 * specialist. Read from the session cookie on the server, so the right
 * buttons arrive with the page; the rest of the page still prerenders.
 */
export function AccountLinks(props: AccountLinksProps) {
  return (
    // Holds the guest buttons' space, unseen, so nothing shifts
    <Suspense fallback={<GuestLinks {...props} placeholder />}>
      <SessionLinks {...props} />
    </Suspense>
  )
}

async function SessionLinks({
  className,
  size = "default",
}: AccountLinksProps) {
  const session = await getCookieSession()
  if (session?.kind !== "session") {
    return <GuestLinks className={className} size={size} />
  }
  const t = await getTranslations("Navbar")

  return (
    <div className={className}>
      <SignOutButton size={size} />
      {session.role === "specialist" && (
        <Link
          href={homePathFor(session.role)}
          className={buttonVariants({ size })}
        >
          <LayoutDashboardIcon data-icon="inline-start" />
          {t("dashboard")}
        </Link>
      )}
    </div>
  )
}

/** Log in and sign up, as a pair of buttons */
function GuestLinks({
  className,
  size = "default",
  placeholder = false,
}: AccountLinksProps & { placeholder?: boolean }) {
  const t = useTranslations("Navbar")

  return (
    <div
      className={cn(className, placeholder && "invisible")}
      aria-hidden={placeholder || undefined}
    >
      <Link
        href="/login"
        className={buttonVariants({ variant: "ghost", size })}
        tabIndex={placeholder ? -1 : undefined}
      >
        {t("logIn")}
      </Link>
      <Link
        href="/signup"
        className={buttonVariants({ size })}
        tabIndex={placeholder ? -1 : undefined}
      >
        {t("join")}
      </Link>
    </div>
  )
}
