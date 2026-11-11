import { buttonVariants } from "@workspace/ui/components/button"
import { Logo } from "@workspace/ui/components/logo"
import { SheetFooter } from "@workspace/ui/components/sheet"
import { ThemeToggle } from "@workspace/ui/components/theme-toggle"
import { useLocale, useTranslations } from "next-intl"

import { LocaleSwitcher } from "@/components/locale-switcher"
import { MobileMenu } from "@/components/navbar/mobile-menu"
import { NavLinks } from "@/components/navbar/nav-links"
import { Link } from "@/i18n/navigation"

/**
 * The public site's top bar, pinned while the page scrolls. From the xl
 * breakpoint (room for the longest language, French) everything sits in the
 * bar; below it the links, account actions and settings move into the burger
 * menu, leaving the logo and the button.
 */
export function Navbar() {
  const t = useTranslations()
  const locale = useLocale()

  return (
    <header className="sticky top-0 z-40 border-b bg-background/80 backdrop-blur supports-backdrop-filter:bg-background/60">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-4 px-4 sm:px-6 xl:gap-8">
        <Link href="/" aria-label={t("Metadata.title")} className="shrink-0">
          <Logo locale={locale === "ar" ? "ar" : "en"} className="h-12" />
        </Link>

        <nav aria-label={t("Navbar.mainNav")} className="hidden xl:block">
          <NavLinks variant="bar" />
        </nav>

        <div className="ms-auto hidden items-center gap-1 xl:flex">
          <LocaleSwitcher compact />
          <ThemeToggle label={t("Common.toggleTheme")} />
          <AccountLinks className="ms-3 flex items-center gap-2" />
        </div>

        <MobileMenu className="ms-auto xl:hidden">
          <nav aria-label={t("Navbar.mainNav")} className="px-4">
            <NavLinks variant="menu" />
          </nav>
          <SheetFooter className="gap-4 border-t">
            <AccountLinks className="flex flex-col gap-2" size="lg" />
            <div className="flex items-center justify-between">
              <LocaleSwitcher />
              <ThemeToggle label={t("Common.toggleTheme")} />
            </div>
          </SheetFooter>
        </MobileMenu>
      </div>
    </header>
  )
}

/** Log in and sign up, as a pair of buttons */
function AccountLinks({
  className,
  size = "default",
}: {
  className?: string
  size?: "default" | "lg"
}) {
  const t = useTranslations("Navbar")

  return (
    <div className={className}>
      <Link
        href="/login"
        className={buttonVariants({ variant: "ghost", size })}
      >
        {t("logIn")}
      </Link>
      <Link href="/signup" className={buttonVariants({ size })}>
        {t("join")}
      </Link>
    </div>
  )
}
