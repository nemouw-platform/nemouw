import { Logo } from "@workspace/ui/components/logo"
import { cn } from "@workspace/ui/lib/utils"
import {
  BadgeCheckIcon,
  BatteryFullIcon,
  ClockIcon,
  MapPinIcon,
  MenuIcon,
  SearchIcon,
  SignalIcon,
  WifiIcon,
} from "lucide-react"
import { useLocale, useTranslations } from "next-intl"

const PEOPLE = ["rana", "karim", "maya"] as const
type Person = (typeof PEOPLE)[number]

/**
 * Illustration of the patient-facing search, drawn in HTML so it follows the
 * theme and the locale. Decorative only: hidden from assistive technology.
 */
export function PhoneMockup({ className }: { className?: string }) {
  const t = useTranslations("Auth.phone")
  const locale = useLocale()

  return (
    <div
      aria-hidden
      className={cn(
        "relative w-75 rounded-[2.75rem] bg-device p-2.5 shadow-2xl ring-1 ring-white/15 select-none",
        className
      )}
    >
      <div className="absolute inset-s-1/2 top-4 z-10 h-6 w-24 -translate-x-1/2 rounded-full bg-device rtl:translate-x-1/2" />

      <div className="flex h-full flex-col gap-3 overflow-hidden rounded-[2.25rem] bg-background px-4 pt-3 pb-6 text-foreground">
        <StatusBar />

        <div className="flex items-center justify-between">
          <Logo locale={locale === "ar" ? "ar" : "en"} className="h-9" />
          <span className="grid size-8 place-items-center rounded-full bg-card shadow-xs">
            <MenuIcon className="size-4" />
          </span>
        </div>

        <div className="flex items-center gap-2 rounded-full bg-card px-3.5 py-2.5 text-xs shadow-xs">
          <SearchIcon className="size-3.5 text-muted-foreground" />
          <span className="truncate">{t("search")}</span>
        </div>

        <div className="flex gap-1.5 text-[11px] font-medium">
          <span className="rounded-full bg-primary px-2.5 py-1 text-primary-foreground">
            {t("filterLanguage")}
          </span>
          <span className="rounded-full bg-card px-2.5 py-1 shadow-xs">
            {t("filterWeek")}
          </span>
        </div>

        <p className="pt-1 text-sm font-semibold">{t("results")}</p>

        <ul className="flex flex-col gap-2.5">
          {PEOPLE.map((person) => (
            <SpecialistCard key={person} person={person} />
          ))}
        </ul>
      </div>
    </div>
  )
}

function StatusBar() {
  return (
    <div className="flex items-center justify-between px-2 text-[11px] font-semibold">
      <span dir="ltr">9:41</span>
      <span className="flex items-center gap-1">
        <SignalIcon className="size-3" />
        <WifiIcon className="size-3" />
        <BatteryFullIcon className="size-3.5" />
      </span>
    </div>
  )
}

function SpecialistCard({ person }: { person: Person }) {
  const t = useTranslations("Auth.phone")
  const p = (key: "name" | "initials" | "role" | "place" | "slot") =>
    t(`people.${person}.${key}`)

  return (
    <li className="flex flex-col gap-2.5 rounded-2xl bg-card p-3 shadow-xs">
      <div className="flex items-center gap-2.5">
        <span className="grid size-9 shrink-0 place-items-center rounded-full bg-secondary text-[11px] font-semibold text-secondary-foreground">
          {p("initials")}
        </span>
        <div className="min-w-0">
          <p className="flex items-center gap-1 text-[13px] font-semibold">
            <span className="truncate">{p("name")}</span>
            <BadgeCheckIcon className="size-3.5 shrink-0 text-primary" />
          </p>
          <p className="text-[11px] text-muted-foreground">{p("role")}</p>
        </div>
      </div>

      <div className="flex flex-col gap-1 text-[11px] text-muted-foreground">
        <span className="flex items-center gap-1.5">
          <MapPinIcon className="size-3" />
          {p("place")}
        </span>
        <span className="flex items-center gap-1.5">
          <ClockIcon className="size-3" />
          {t("nextSlot", { slot: p("slot") })}
        </span>
      </div>

      <div className="flex items-center justify-between gap-2">
        <span dir="ltr" className="flex gap-1 text-[10px] font-medium">
          {["AR", "FR", "EN"].map((code) => (
            <span key={code} className="rounded-md bg-secondary px-1.5 py-0.5">
              {code}
            </span>
          ))}
        </span>
        <span className="rounded-lg bg-primary px-2.5 py-1 text-[11px] font-medium text-primary-foreground">
          {t("viewProfile")}
        </span>
      </div>
    </li>
  )
}
