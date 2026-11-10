import { buttonVariants } from "@workspace/ui/components/button"
import { useTranslations } from "next-intl"

import { Link } from "@/i18n/navigation"

export default function NotFound() {
  const t = useTranslations("NotFound")

  return (
    <main className="flex min-h-svh flex-col items-center justify-center gap-4 p-6 text-center">
      <h1 className="text-2xl font-semibold">{t("title")}</h1>
      <p className="text-muted-foreground">{t("description")}</p>
      <Link href="/" className={buttonVariants({ variant: "outline" })}>
        {t("backHome")}
      </Link>
    </main>
  )
}
