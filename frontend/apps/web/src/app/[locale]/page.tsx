import { useTranslations } from "next-intl"

import { Navbar } from "@/components/navbar"

export default function Page() {
  const t = useTranslations("HomePage")

  return (
    <>
      <Navbar />
      <main className="mx-auto flex min-h-[calc(100svh-4rem)] max-w-6xl items-center justify-center px-4 sm:px-6">
        <p className="text-sm text-muted-foreground">{t("tagline")}</p>
      </main>
    </>
  )
}
