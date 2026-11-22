import { Alert, AlertDescription } from "@workspace/ui/components/alert"
import { CircleAlertIcon, CircleCheckIcon } from "lucide-react"

interface FormAlertProps {
  children: React.ReactNode
  /** "error" for failures no field explains, "success" for confirmations */
  tone?: "error" | "success"
}

/** Form-level message under the fields; renders nothing without content */
export function FormAlert({ children, tone = "error" }: FormAlertProps) {
  if (!children) return null
  const Icon = tone === "error" ? CircleAlertIcon : CircleCheckIcon

  return (
    <Alert
      variant={tone === "error" ? "destructive" : "default"}
      role={tone === "error" ? "alert" : "status"}
    >
      <Icon />
      <AlertDescription>{children}</AlertDescription>
    </Alert>
  )
}
