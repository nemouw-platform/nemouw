import { getCookieSession } from "@/features/auth/session/server-session"
import { SyndicateProvider } from "@/features/syndicate/me/syndicate-provider"

/**
 * The syndicate area's data: who is signed in (from the session cookie, on
 * the server), then their syndicate. Reads the request: render it inside
 * <Suspense>.
 */
export async function SyndicateArea({
  fallback,
  children,
}: {
  fallback: React.ReactNode
  children: React.ReactNode
}) {
  const session = await getCookieSession()
  const userId = session?.kind === "session" ? session.userId : null
  return (
    <SyndicateProvider userId={userId} fallback={fallback}>
      {children}
    </SyndicateProvider>
  )
}
