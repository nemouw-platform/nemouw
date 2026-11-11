"use client"

import { Button } from "@workspace/ui/components/button"
import { Input } from "@workspace/ui/components/input"
import { cn } from "@workspace/ui/lib/utils"
import { EyeIcon, EyeOffIcon } from "lucide-react"
import { useTranslations } from "next-intl"
import * as React from "react"

import {
  FormField,
  splitFieldProps,
  type FieldOwnProps,
} from "@/features/auth/components/form-field"

type InputProps = Omit<React.ComponentProps<typeof Input>, "type">

/** Password input with a show / hide toggle at its end */
export function PasswordInput({ className, ...props }: InputProps) {
  const t = useTranslations("Auth.password")
  const [visible, setVisible] = React.useState(false)

  return (
    // Passwords are typed left to right, even on Arabic pages. The wrapper (not
    // just the input) is LTR so the eye button stays on the side the padding
    // reserves for it.
    <div dir="ltr" className="relative">
      <Input
        type={visible ? "text" : "password"}
        className={cn("h-10 bg-card pe-10", className)}
        {...props}
      />
      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        // Centered with margins, not translate: Button's press effect
        // (active:translate-y-px) would replace a -50% translate and make it
        // jump. The press nudge is turned off too, so only the icon changes.
        className="inset-e-1.5 absolute inset-y-0 my-auto text-muted-foreground active:not-aria-[haspopup]:translate-y-0"
        aria-label={visible ? t("hide") : t("show")}
        aria-pressed={visible}
        onClick={() => setVisible((shown) => !shown)}
      >
        <span className="relative size-4">
          <FadeIcon icon={EyeIcon} shown={!visible} />
          <FadeIcon icon={EyeOffIcon} shown={visible} />
        </span>
      </Button>
    </div>
  )
}

/** Both icons stay mounted on top of each other and cross-fade */
function FadeIcon({
  icon: Icon,
  shown,
}: {
  icon: React.ComponentType<{ className?: string }>
  shown: boolean
}) {
  return (
    <Icon
      className={cn(
        "absolute inset-0 size-4 transition-opacity duration-200 ease-out motion-reduce:transition-none",
        shown ? "opacity-100" : "opacity-0"
      )}
    />
  )
}

/** FormField with a PasswordInput; `className` styles the input */
export function FormPasswordField(
  props: FieldOwnProps & Omit<InputProps, "id">
) {
  const [field, inputProps] = splitFieldProps(props)
  return (
    <FormField {...field}>
      {(control) => <PasswordInput {...control} {...inputProps} />}
    </FormField>
  )
}
