"use client"

import { localeDirection } from "@workspace/i18n"
import { Button } from "@workspace/ui/components/button"
import { Logo } from "@workspace/ui/components/logo"
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@workspace/ui/components/sheet"
import { MenuIcon, XIcon } from "lucide-react"
import { useLocale, useTranslations } from "next-intl"
import * as React from "react"

interface MobileMenuProps {
  /** The drawer's content: the links and the account actions */
  children: React.ReactNode
  className?: string
}

/**
 * The burger button and the drawer it opens. The drawer slides in from the
 * button's side (the end of the line, so the left in Arabic) and closes when
 * a link inside it is followed, including a "#section" on the same page.
 */
export function MobileMenu({ children, className }: MobileMenuProps) {
  const [open, setOpen] = React.useState(false)
  const t = useTranslations("Navbar")
  const locale = useLocale()
  const side = localeDirection(locale) === "rtl" ? "left" : "right"

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger
        render={
          <Button
            variant="ghost"
            size="icon-lg"
            aria-label={t("openMenu")}
            className={className}
          />
        }
      >
        <MenuIcon className="size-5" />
      </SheetTrigger>
      <SheetContent
        side={side}
        showCloseButton={false}
        onClick={(event) => {
          if ((event.target as Element).closest("a")) setOpen(false)
        }}
      >
        {/* Same height and logo as the navbar, so the top lines up with it */}
        <SheetHeader className="h-16 flex-row items-center justify-between py-0">
          <SheetTitle>
            <Logo locale={locale === "ar" ? "ar" : "en"} className="h-12" />
            <span className="sr-only">{t("menu")}</span>
          </SheetTitle>
          <SheetClose
            render={
              <Button
                variant="ghost"
                size="icon-lg"
                aria-label={t("closeMenu")}
              />
            }
          >
            <XIcon className="size-5" />
          </SheetClose>
        </SheetHeader>
        {children}
      </SheetContent>
    </Sheet>
  )
}
