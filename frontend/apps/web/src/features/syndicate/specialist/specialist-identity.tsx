import type { SpecialistResponse } from "@workspace/api"
import { cn } from "@workspace/ui/lib/utils"
import { UserRoundIcon } from "lucide-react"
import { useTranslations } from "next-intl"

/**
 * Who a specialist is, as far as reviewers can tell: their photo, username
 * and member number (the backend does not give reviewers their name yet)
 */
export function SpecialistIdentity({
  specialist,
  size = "default",
}: {
  specialist: SpecialistResponse | undefined
  size?: "default" | "lg"
}) {
  const t = useTranslations("Syndicate.specialist")
  const photo = specialist?.profile_picture_url
  const frame = size === "lg" ? "size-16" : "size-11"

  return (
    <div className="flex min-w-0 items-center gap-3">
      <span
        className={cn(
          "flex shrink-0 items-center justify-center overflow-hidden rounded-full bg-muted ring-1 ring-foreground/10",
          frame
        )}
      >
        {photo ? (
          // A CDN image, shown small
          // eslint-disable-next-line @next/next/no-img-element
          <img src={photo} alt="" className="size-full object-cover" />
        ) : (
          <UserRoundIcon
            aria-hidden
            className="size-1/2 text-muted-foreground"
          />
        )}
      </span>
      <span className="flex min-w-0 flex-col">
        <span
          dir="ltr"
          className={cn(
            "truncate text-start font-medium",
            size === "lg" && "text-lg"
          )}
        >
          @{specialist?.username ?? "…"}
        </span>
        <span className="text-xs text-muted-foreground">
          {t("memberNumber", {
            number: specialist?.syndicate_member_number ?? "—",
          })}
        </span>
      </span>
    </div>
  )
}
