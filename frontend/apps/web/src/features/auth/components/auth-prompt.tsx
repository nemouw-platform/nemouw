import { cn } from "@workspace/ui/lib/utils"

import { Link } from "@/i18n/navigation"

const ACTION_CLASS =
  "font-medium text-foreground underline underline-offset-4 hover:text-primary disabled:cursor-not-allowed disabled:no-underline disabled:opacity-60"

/** A muted question with an action after it: "Already have an account? Log in" */
export function AuthPrompt({
  prompt,
  children,
}: {
  prompt: React.ReactNode
  children: React.ReactNode
}) {
  return (
    <p className="text-center text-sm text-muted-foreground">
      {prompt} {children}
    </p>
  )
}

/** Underlined text link used in prompts and inline error messages */
export function AuthPromptLink({
  className,
  ...props
}: React.ComponentProps<typeof Link>) {
  return <Link className={cn(ACTION_CLASS, className)} {...props} />
}

/** Same look as AuthPromptLink, for actions that are not navigation (e.g. "Resend code") */
export function AuthPromptButton({
  className,
  ...props
}: React.ComponentProps<"button">) {
  return (
    <button type="button" className={cn(ACTION_CLASS, className)} {...props} />
  )
}
