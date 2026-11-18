import { useTranslations } from "next-intl"

import { PageHeader } from "@/components/page-header"

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
    <PageHeader
      title={title}
      description={
        (step || description) && (
          <>
            {step && <>{t("step", step)} </>}
            {description}
          </>
        )
      }
    />
  )
}
