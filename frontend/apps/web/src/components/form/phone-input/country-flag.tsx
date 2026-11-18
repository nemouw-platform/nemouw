import { cn } from "@workspace/ui/lib/utils"
import type { CountryCode } from "libphonenumber-js"

/**
 * A country's flag (3:2), from the /flags route. An image rather than an
 * emoji: Windows shows flag emoji as two letters. Decorative: the country's
 * name or code is always written next to it.
 */
export function CountryFlag({
  country,
  className,
}: {
  country: CountryCode
  className?: string
}) {
  return (
    // A tiny static SVG: next/image would add nothing but config for SVGs
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={`/flags/${country}.svg`}
      alt=""
      width={21}
      height={14}
      loading="lazy"
      decoding="async"
      className={cn(
        "h-3.5 w-[21px] shrink-0 rounded-[2px] object-cover ring-1 ring-foreground/10",
        className
      )}
    />
  )
}
