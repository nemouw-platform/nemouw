"use client"

import type { SyndicateResponse, SyndicateStaffResponse } from "@workspace/api"
import { Card, CardContent } from "@workspace/ui/components/card"
import { useLoad } from "@workspace/ui/hooks/use-load"
import { Building2Icon } from "lucide-react"
import { useTranslations } from "next-intl"
import * as React from "react"

import { LoadError } from "@/components/load-error"
import { findMemberships } from "@/features/syndicate/me/load-memberships"
import { syndicatesApi } from "@/lib/api"
import { generalErrorMessage } from "@/lib/forms/api-errors"

interface SyndicateContextValue {
  syndicate: SyndicateResponse
  /** Swap in the syndicate a save returned */
  setSyndicate: (syndicate: SyndicateResponse) => void
  /** The signed-in user's place on its staff */
  member: SyndicateStaffResponse
  /** Only the owner manages staff and the syndicate's details */
  isOwner: boolean
}

const SyndicateContext = React.createContext<SyndicateContextValue | null>(null)

/** The user's first syndicate, with its details; null when on none */
async function loadSyndicate(userId: string, signal: AbortSignal) {
  const [membership] = await findMemberships(userId, signal)
  if (!membership) return null
  const syndicate = await syndicatesApi.get(membership.syndicateId, { signal })
  return { syndicate, member: membership.member }
}

/**
 * Finds the signed-in staff member's syndicate once, for every page under
 * it. Shows `fallback` meanwhile, and says so when the account is on no
 * syndicate's staff. (A user on several syndicates gets the first one.)
 */
export function SyndicateProvider({
  userId,
  fallback,
  children,
}: {
  /** From the session cookie, read on the server */
  userId: string | null
  fallback: React.ReactNode
  children: React.ReactNode
}) {
  const t = useTranslations("Syndicate")
  const tFormErrors = useTranslations("Form.errors")
  const [syndicate, setSyndicate] = React.useState<SyndicateResponse | null>(
    null
  )
  const load = useLoad(
    async (signal) => (userId ? loadSyndicate(userId, signal) : null),
    { onSuccess: (found) => found && setSyndicate(found.syndicate) }
  )

  const member = load.data?.member
  const value = React.useMemo(
    () =>
      syndicate && member
        ? {
            syndicate,
            setSyndicate,
            member,
            isOwner: Boolean(member.is_owner),
          }
        : null,
    [syndicate, member]
  )

  if (load.failed) {
    return (
      <LoadError
        message={generalErrorMessage(load.error, tFormErrors)}
        onRetry={load.retry}
      />
    )
  }
  if (load.isLoading) return fallback
  if (!value) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center gap-3 py-10 text-center">
          <Building2Icon aria-hidden className="size-8 text-muted-foreground" />
          <p className="font-medium">{t("noSyndicate.title")}</p>
          <p className="max-w-sm text-sm text-pretty text-muted-foreground">
            {t("noSyndicate.description")}
          </p>
        </CardContent>
      </Card>
    )
  }
  return <SyndicateContext value={value}>{children}</SyndicateContext>
}

/** The signed-in staff member's syndicate, inside SyndicateProvider */
export function useSyndicate(): SyndicateContextValue {
  const value = React.useContext(SyndicateContext)
  if (!value)
    throw new Error("useSyndicate must be used inside SyndicateProvider")
  return value
}
