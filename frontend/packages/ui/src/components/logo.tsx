import { cn } from "cn"

import logo from "../assets/logo.png"

type LogoProps = Omit<
  React.ComponentProps<"img">,
  "src" | "alt" | "width" | "height"
>

// Raster PNG cut out of the brand JPEG. Replace with an inline SVG
// component once a real vector logo exists (sharper, theme-aware colors).
function Logo({ className, ...props }: LogoProps) {
  return (
    <img
      src={logo.src}
      width={logo.width}
      height={logo.height}
      alt="Nemouw"
      className={cn("h-10 w-auto", className)}
      {...props}
    />
  )
}

export { Logo }
