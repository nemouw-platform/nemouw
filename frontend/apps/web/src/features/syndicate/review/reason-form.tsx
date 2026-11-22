"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { Button } from "@workspace/ui/components/button"
import { CircleXIcon } from "lucide-react"
import { useTranslations } from "next-intl"
import * as React from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { FormTextareaField } from "@/components/form/form-field"
import { SubmitButton } from "@/components/form/submit-button"
import { FormCard } from "@/features/dashboard/form-card"
import { FormDialog } from "@/features/dashboard/form-dialog"
import { createFieldRules } from "@/lib/forms/validation"
import { useFormRequest } from "@/lib/forms/use-form-request"

/** The backend's ValidationLimits.DESCRIPTION_MAX */
const REASON_MAX_LENGTH = 1000

interface ReasonFormProps {
  title: string
  description: string
  submitLabel: string
  /** Sends the decision with its reason */
  onSubmit: (reason: string, signal: AbortSignal) => Promise<unknown>
  onDone: () => void
  onCancel: () => void
}

/**
 * Asks why, for a decision the other side reads (rejecting a credential or
 * a specialist): the specialist sees this reason in their dashboard
 */
export function ReasonForm({
  title,
  description,
  submitLabel,
  onSubmit,
  onDone,
  onCancel,
}: ReasonFormProps) {
  const t = useTranslations("Syndicate.reason")
  const tFormErrors = useTranslations("Form.errors")
  const schema = React.useMemo(
    () =>
      z.object({
        reason: createFieldRules(tFormErrors).text(
          t("label"),
          REASON_MAX_LENGTH
        ),
      }),
    [t, tFormErrors]
  )
  const form = useForm<{ reason: string }>({
    resolver: zodResolver(schema),
    defaultValues: { reason: "" },
    mode: "onTouched",
  })
  const save = useFormRequest(
    (signal, reason: string) => onSubmit(reason, signal),
    { onSuccess: onDone }
  )

  return (
    <FormCard
      title={title}
      description={description}
      onSubmit={form.handleSubmit(({ reason }) => save.run(reason))}
      formError={save.formError}
      cancel={{ label: t("cancel"), onClick: onCancel }}
      submit={
        <SubmitButton
          pending={save.isPending}
          pendingLabel={t("sending")}
          size="default"
        >
          {submitLabel}
        </SubmitButton>
      }
    >
      <FormTextareaField
        id="reason"
        required
        label={t("label")}
        placeholder={t("placeholder")}
        description={t("hint")}
        rows={4}
        error={form.formState.errors.reason}
        {...form.register("reason")}
      />
    </FormCard>
  )
}

type RejectButtonProps = Omit<ReasonFormProps, "onDone" | "onCancel"> & {
  /** The button's label, e.g. "Reject" */
  label: string
}

/**
 * "Reject", which asks why in a popup before it sends anything. `onSubmit`
 * sends it and keeps what the backend returns; the popup then closes.
 */
export function RejectButton({ label, ...form }: RejectButtonProps) {
  const [open, setOpen] = React.useState(false)

  return (
    <>
      <Button variant="outline" size="sm" onClick={() => setOpen(true)}>
        <CircleXIcon data-icon="inline-start" />
        {label}
      </Button>
      <FormDialog open={open} onClose={() => setOpen(false)}>
        <ReasonForm
          {...form}
          onDone={() => setOpen(false)}
          onCancel={() => setOpen(false)}
        />
      </FormDialog>
    </>
  )
}
