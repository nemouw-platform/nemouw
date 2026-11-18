import { SheetFooter } from "@workspace/ui/components/sheet"
import { ThemeToggle } from "@workspace/ui/components/theme-toggle"
import { useTranslations } from "next-intl"

import { LocaleSwitcher } from "@/components/locale-switcher"
import { AccountLinks } from "@/components/navbar/account-links"
import { MobileMenu } from "@/components/navbar/mobile-menu"
import { NavLinks } from "@/components/navbar/nav-links"
import { SiteHeader } from "@/components/site-header"

/**
 * The public site's top bar, pinned while the page scrolls. From the xl
 * breakpoint (room for the longest language, French) everything sits in the
 * bar; below it the links, account actions and settings move into the burger
 * menu, leaving the logo and the button.
 */
export function Navbar() {
  const t = useTranslations()

  return (
    <SiteHeader>
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
    </SiteHeader>
  )
}
