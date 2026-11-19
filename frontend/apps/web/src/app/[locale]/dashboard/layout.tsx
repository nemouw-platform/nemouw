import { DashboardShell } from "@/features/dashboard/dashboard-shell"

/** Every dashboard page; the proxy lets only signed-in specialists in */
export default function DashboardLayout({
  children,
}: LayoutProps<"/[locale]/dashboard">) {
  return <DashboardShell>{children}</DashboardShell>
}
