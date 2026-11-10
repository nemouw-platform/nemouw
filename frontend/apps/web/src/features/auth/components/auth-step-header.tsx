import { useTranslations } from "next-intl"

interface AuthStepHeaderProps {
  title: React.ReactNode
  description?: React.ReactNode
  /** Shown as "Step 1 of 3." before the description */
  step?: { current: number; total: number }
}

/** Title and helper line at the top of every auth screen */
export function AuthStepHeader({
  title,
  description,
  step,
}: AuthStepHeaderProps) {
  const t = useTranslations("Auth")

  return (
    <div className="flex flex-col gap-2">
      <h1 className="text-2xl font-semibold tracking-tight text-balance">
        {title}
      </h1>
      {(step || description) && (
        <p className="text-sm text-pretty text-muted-foreground">
          {step && <>{t("step", step)} </>}
          {description}
        </p>
      )}
    </div>
  )
}
