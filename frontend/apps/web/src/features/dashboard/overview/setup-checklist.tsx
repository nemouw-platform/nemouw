"use client"

import { LoadError } from "@/components/load-error"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
} from "@workspace/ui/components/card"
import { Skeleton } from "@workspace/ui/components/skeleton"
import { cn } from "@workspace/ui/lib/utils"
import { ChevronRightIcon, CircleCheckIcon, CircleIcon } from "lucide-react"
import { useTranslations } from "next-intl"

import { CardHeading } from "@/features/dashboard/card-heading"
import { setupSteps } from "@/features/dashboard/overview/setup-steps"
import { useProfileContent } from "@/features/specialists/about/use-profile-content"
import { useSpecialist } from "@/features/specialists/me/specialist-provider"
import { Link } from "@/i18n/navigation"
import { generalErrorMessage } from "@/lib/forms/api-errors"

/** What is left to fill in, with a link to each part */
export function SetupChecklist() {
  const t = useTranslations("Dashboard.setup")
  const tFormErrors = useTranslations("Form.errors")
  const { specialist } = useSpecialist()
  const { content, isLoading, failed, error, retry } = useProfileContent()

  const steps = setupSteps(specialist, content)
  const doneCount = steps.filter((step) => step.done).length
  const complete = doneCount === steps.length

  return (
    <Card className="[--card-spacing:--spacing(6)]">
      <CardHeader>
        <CardHeading>{complete ? t("completeTitle") : t("title")}</CardHeading>
        <CardDescription>
          {t("progress", { done: doneCount, total: steps.length })}
        </CardDescription>
      </CardHeader>
      <CardContent>
        {failed ? (
          <LoadError
            message={generalErrorMessage(error, tFormErrors)}
            onRetry={retry}
          />
        ) : isLoading ? (
          <Skeleton className="h-32 w-full" />
        ) : (
          <ol className="-mx-3 flex flex-col">
            {steps.map(({ key, done, href }) => {
              const Icon = done ? CircleCheckIcon : CircleIcon
              return (
                <li key={key}>
                  <Link
                    href={href}
                    className="group flex items-center gap-3 rounded-lg px-3 py-3 outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50"
                  >
                    <Icon
                      aria-hidden
                      className={cn(
                        "size-5 shrink-0",
                        done ? "text-primary" : "text-muted-foreground"
                      )}
                    />
                    <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                      <span
                        className={cn(
                          "font-medium",
                          done && "text-muted-foreground line-through"
                        )}
                      >
                        {t(`steps.${key}.title`)}
                        <span className="sr-only">
                          {" "}
                          ({done ? t("done") : t("toDo")})
                        </span>
                      </span>
                      <span className="text-muted-foreground">
                        {t(`steps.${key}.description`)}
                      </span>
                    </span>
                    <ChevronRightIcon
                      aria-hidden
                      className="size-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-0.5 rtl:-scale-x-100 rtl:group-hover:-translate-x-0.5"
                    />
                  </Link>
                </li>
              )
            })}
          </ol>
        )}
      </CardContent>
    </Card>
  )
}
