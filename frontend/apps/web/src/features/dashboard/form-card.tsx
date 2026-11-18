import { FormAlert } from "@/components/form/form-alert"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
} from "@workspace/ui/components/card"
import { cn } from "@workspace/ui/lib/utils"
import { CircleCheckIcon } from "lucide-react"

import { CardHeading } from "@/features/dashboard/card-heading"

interface FormCardProps {
  title: React.ReactNode
  description?: React.ReactNode
  onSubmit: React.FormEventHandler<HTMLFormElement>
  /** The fields */
  children: React.ReactNode
  /** The save button */
  submit: React.ReactNode
  /** A failure no field explains */
  formError?: React.ReactNode
  /** Shown beside the button after a successful save, e.g. "Saved" */
  savedMessage?: React.ReactNode
  className?: string
}

/**
 * One dashboard form as a card: what it is for, its fields, then the save
 * button with the outcome of the last save.
 */
export function FormCard({
  title,
  description,
  onSubmit,
  children,
  submit,
  formError,
  savedMessage,
  className,
}: FormCardProps) {
  return (
    <Card className={cn("[--card-spacing:--spacing(6)]", className)}>
      <form noValidate onSubmit={onSubmit} className="contents">
        <CardHeader>
          <CardHeading>{title}</CardHeading>
          {description && <CardDescription>{description}</CardDescription>}
        </CardHeader>
        <CardContent className="flex flex-col gap-5">
          {children}
          <FormAlert>{formError}</FormAlert>
        </CardContent>
        <CardFooter className="justify-end gap-3 border-t py-4">
          {savedMessage && (
            <p
              role="status"
              className="flex items-center gap-1.5 text-sm text-muted-foreground"
            >
              <CircleCheckIcon aria-hidden className="size-4 text-primary" />
              {savedMessage}
            </p>
          )}
          {submit}
        </CardFooter>
      </form>
    </Card>
  )
}
