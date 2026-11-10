import { cn } from "cn"

type LogoProps = Omit<
  React.ComponentProps<"img">,
  "src" | "alt" | "width" | "height"
> & {
  locale?: "en" | "ar"
}

// Served from each app's public folder (logo_en.svg / logo_ar.svg).
// The SVGs share a 2:1 viewBox, so width/height only reserve layout space.
function Logo({ locale = "en", className, ...props }: LogoProps) {
  return (
    <img
      src={`/logo_${locale}.svg`}
      width={200}
      height={100}
      alt="Nemouw"
      className={cn("h-10 w-auto", className)}
      {...props}
    />
  )
}

export { Logo }
