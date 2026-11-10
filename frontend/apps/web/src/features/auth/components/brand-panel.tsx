import { cn } from "@workspace/ui/lib/utils"
import { useTranslations } from "next-intl"

import { PhoneMockup } from "@/features/auth/components/phone-mockup"

/**
 * Purple brand side of the auth screens: headline, one line of copy, and a
 * phone showing what patients see, cropped by the panel's bottom edge.
 */
export function BrandPanel({ className }: { className?: string }) {
  const t = useTranslations("Auth.brand")

  return (
    <aside
      className={cn(
        "relative isolate flex-col overflow-hidden rounded-3xl bg-brand text-brand-foreground",
        className
      )}
    >
      <div className="flex max-w-xl flex-col gap-5 p-10 pb-0 xl:p-14 xl:pb-0">
        <h2 className="text-4xl leading-tight font-semibold tracking-tight text-balance xl:text-5xl">
          {t.rich("headline", {
            em: (chunks) => <span className="text-brand-accent">{chunks}</span>,
          })}
        </h2>
        <p className="max-w-lg text-base/7 text-pretty text-brand-foreground/80">
          {t("description")}
        </p>
      </div>

      {/* The whole phone shows: the panel grows to fit it and the page scrolls */}
      <div className="relative mt-12 flex flex-1 items-end justify-center px-10 pb-14 xl:px-14">
        <Stripes />
        <PhoneMockup />
      </div>
    </aside>
  )
}

/** Diagonal bands in the logo's leaf green behind the phone */
function Stripes() {
  return (
    <div aria-hidden className="pointer-events-none absolute inset-0 -z-10">
      {/* Sage over purple turns grey when faint, so: a soft white band plus a clearly green one */}
      <div className="absolute -start-1/4 bottom-28 h-28 w-[150%] -rotate-[24deg] rounded-full bg-white/[0.07] rtl:rotate-[24deg]" />
      <div className="absolute -start-1/4 bottom-12 h-3 w-[150%] -rotate-[24deg] rounded-full bg-brand-accent rtl:rotate-[24deg]" />
    </div>
  )
}
