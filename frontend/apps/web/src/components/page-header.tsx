interface PageHeaderProps {
  title: React.ReactNode
  description?: React.ReactNode
  /** Shown at the end of the title row, e.g. a status badge */
  action?: React.ReactNode
}

/** The page's h1 and the helper line under it */
export function PageHeader({ title, description, action }: PageHeaderProps) {
  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight text-balance">
          {title}
        </h1>
        {action}
      </div>
      {description && (
        <p className="text-sm text-pretty text-muted-foreground">
          {description}
        </p>
      )}
    </div>
  )
}
