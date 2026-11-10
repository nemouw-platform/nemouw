import { Logo } from "@workspace/ui/components/logo"
import { ThemeToggle } from "@workspace/ui/components/theme-toggle"

export default function Page() {
  return (
    <div className="flex min-h-svh p-6">
      <div className="flex max-w-fit m-auto min-w-0 flex-col gap-4 text-sm leading-loose">
        <div>
          <h1 className="mb-2">
            <Logo className="h-16" />
          </h1>
          <p>Staff dashboard: credential review and moderation.</p>
          <div className="mt-2">
            <ThemeToggle />
          </div>
        </div>
      </div>
    </div>
  )
}
