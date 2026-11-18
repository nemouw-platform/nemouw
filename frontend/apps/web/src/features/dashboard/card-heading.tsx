import { CardTitle } from "@workspace/ui/components/card"

/**
 * A dashboard card's title. CardTitle is a plain div; this makes it the h2
 * under the page's h1, so screen readers can jump between cards.
 */
export function CardHeading({ children }: { children: React.ReactNode }) {
  return (
    <CardTitle role="heading" aria-level={2} className="text-base">
      {children}
    </CardTitle>
  )
}
