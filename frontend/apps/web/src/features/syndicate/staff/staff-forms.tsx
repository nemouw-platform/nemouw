"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { isApiError, type SyndicateStaffResponse } from "@workspace/api"
import { useTranslations } from "next-intl"
import * as React from "react"
import { useForm } from "react-hook-form"

import { FormTextField } from "@/components/form/form-field"
import { SubmitButton } from "@/components/form/submit-button"
import { FormCard } from "@/features/dashboard/form-card"
import {
  createInviteSchema,
  createTitleSchema,
  EMPTY_INVITE,
  STAFF_FIELDS,
  toInviteRequest,
  type InviteValues,
} from "@/features/syndicate/staff/staff-schema"
import { syndicatesApi } from "@/lib/api"
import { applyFieldErrors } from "@/lib/forms/api-errors"
import { useFormRequest } from "@/lib/forms/use-form-request"

interface StaffFormProps {
  syndicateId: string
  onSaved: (member: SyndicateStaffResponse) => void
  onCancel: () => void
}

/**
 * Invites someone to the staff: they get an email with a link to set their
 * password (valid 24 hours), and can review once they have
 */
export function InviteForm({ syndicateId, onSaved, onCancel }: StaffFormProps) {
  const t = useTranslations("Syndicate.staff.form")
  const tFormErrors = useTranslations("Form.errors")
  const schema = React.useMemo(
    () =>
      createInviteSchema(
        {
          first_name: t("firstName"),
          last_name: t("lastName"),
          email: t("email"),
          title: t("title"),
        },
        tFormErrors
      ),
    [t, tFormErrors]
  )
  const form = useForm<InviteValues>({
    resolver: zodResolver(schema),
    defaultValues: EMPTY_INVITE,
    mode: "onTouched",
  })
  const save = useFormRequest(
    (signal, values: InviteValues) =>
      syndicatesApi.inviteStaff(syndicateId, toInviteRequest(values), {
        signal,
      }),
    {
      onSuccess: onSaved,
      onError: (error) => {
        if (isApiError(error) && error.code === "EMAIL_TAKEN") {
          form.setError(
            "email",
            { message: t("emailTaken") },
            { shouldFocus: true }
          )
          return true
        }
        return applyFieldErrors(form, error, STAFF_FIELDS, tFormErrors)
      },
    }
  )
  const { errors } = form.formState

  return (
    <FormCard
      title={t("inviteTitle")}
      description={t("inviteDescription")}
      onSubmit={form.handleSubmit((values) => save.run(values))}
      formError={save.formError}
      cancel={{ label: t("cancel"), onClick: onCancel }}
      submit={
        <SubmitButton
          pending={save.isPending}
          pendingLabel={t("sending")}
          size="default"
        >
          {t("invite")}
        </SubmitButton>
      }
    >
      <div className="grid gap-5 sm:grid-cols-2">
        <FormTextField
          id="first_name"
          required
          autoComplete="off"
          label={t("firstName")}
          placeholder={t("firstNamePlaceholder")}
          error={errors.first_name}
          {...form.register("first_name")}
        />
        <FormTextField
          id="last_name"
          required
          autoComplete="off"
          label={t("lastName")}
          placeholder={t("lastNamePlaceholder")}
          error={errors.last_name}
          {...form.register("last_name")}
        />
        <FormTextField
          id="email"
          type="email"
          dir="ltr"
          required
          autoComplete="off"
          label={t("email")}
          placeholder={t("emailPlaceholder")}
          description={t("emailHint")}
          error={errors.email}
          {...form.register("email")}
        />
        <FormTextField
          id="title"
          required
          label={t("title")}
          placeholder={t("titlePlaceholder")}
          error={errors.title}
          {...form.register("title")}
        />
      </div>
    </FormCard>
  )
}

/** Changes a staff member's title, e.g. after a promotion */
export function TitleForm({
  syndicateId,
  member,
  onSaved,
  onCancel,
}: StaffFormProps & { member: SyndicateStaffResponse }) {
  const t = useTranslations("Syndicate.staff.form")
  const tFormErrors = useTranslations("Form.errors")
  const schema = React.useMemo(
    () => createTitleSchema(t("title"), tFormErrors),
    [t, tFormErrors]
  )
  const form = useForm<{ title: string }>({
    resolver: zodResolver(schema),
    defaultValues: { title: member.title ?? "" },
    mode: "onTouched",
  })
  const save = useFormRequest(
    (signal, title: string) =>
      syndicatesApi.updateStaff(syndicateId, member.id ?? "", title, {
        signal,
      }),
    {
      onSuccess: onSaved,
      onError: (error) => applyFieldErrors(form, error, ["title"], tFormErrors),
    }
  )

  return (
    <FormCard
      title={t("editTitle")}
      description={t("editDescription", {
        name: `${member.user?.first_name ?? ""} ${member.user?.last_name ?? ""}`.trim(),
      })}
      onSubmit={form.handleSubmit(({ title }) => save.run(title))}
      formError={save.formError}
      cancel={{ label: t("cancel"), onClick: onCancel }}
      submit={
        <SubmitButton
          pending={save.isPending}
          pendingLabel={t("saving")}
          size="default"
        >
          {t("save")}
        </SubmitButton>
      }
    >
      <FormTextField
        id={`title-${member.id}`}
        required
        label={t("title")}
        placeholder={t("titlePlaceholder")}
        error={form.formState.errors.title}
        {...form.register("title")}
      />
    </FormCard>
  )
}
