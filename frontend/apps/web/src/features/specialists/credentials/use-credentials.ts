"use client"

import type { CredentialResponse } from "@workspace/api"
import { useLoad } from "@workspace/ui/hooks/use-load"
import * as React from "react"

import { credentialsApi } from "@/lib/api"

/**
 * The specialist's credentials, loaded once. `saved` puts in what a create
 * or edit returned (new ones first); `removed` drops a withdrawn one.
 */
export function useCredentials() {
  const [credentials, setCredentials] = React.useState<CredentialResponse[]>([])
  const { isLoading, failed, error, retry } = useLoad(
    (signal) => credentialsApi.list({ signal }),
    { onSuccess: setCredentials }
  )

  const saved = React.useCallback((credential: CredentialResponse) => {
    setCredentials((list) =>
      list.some(({ id }) => id === credential.id)
        ? list.map((item) => (item.id === credential.id ? credential : item))
        : [credential, ...list]
    )
  }, [])

  const removed = React.useCallback((id: string) => {
    setCredentials((list) => list.filter((item) => item.id !== id))
  }, [])

  return { credentials, isLoading, failed, error, retry, saved, removed }
}

/**
 * Opens a credential's private document in a new tab, through a link that
 * lasts a few minutes. The tab opens on the click itself (so pop-up blockers
 * allow it) and is pointed at the link once it arrives.
 */
export function useDocumentLink(credentialId: string | undefined) {
  const [opening, setOpening] = React.useState(false)
  const [failed, setFailed] = React.useState(false)

  const open = React.useCallback(async () => {
    if (!credentialId) return
    const tab = window.open("", "_blank")
    setOpening(true)
    setFailed(false)
    try {
      const { url } = await credentialsApi.documentLink(credentialId)
      if (tab && url) {
        tab.opener = null
        tab.location.href = url
      }
    } catch {
      tab?.close()
      setFailed(true)
    } finally {
      setOpening(false)
    }
  }, [credentialId])

  return { open, opening, failed }
}
