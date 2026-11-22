import { Badge } from "@workspace/ui/components/badge"
import { BadgeCheckIcon, CircleXIcon, ClockIcon } from "lucide-react"

/** Review states: a profile's (pending, active, rejected) and a credential's (verified for active) */
export type ReviewStatus = "pending" | "active" | "verified" | "rejected"

const STATUS = {
  pending: { icon: ClockIcon, variant: "secondary" },
  active: { icon: BadgeCheckIcon, variant: "default" },
  verified: { icon: BadgeCheckIcon, variant: "default" },
  rejected: { icon: CircleXIcon, variant: "destructive" },
} as const

/** Where a review stands, as an icon and a short label */
export function StatusBadge({
  status,
  children,
}: {
  status: ReviewStatus
  /** The label, e.g. "Pending" */
  children: React.ReactNode
}) {
  const { icon: Icon, variant } = STATUS[status]
  return (
    <Badge variant={variant}>
      <Icon data-icon="inline-start" />
      {children}
    </Badge>
  )
}
