"use client"

import type { CredentialResponse, DocumentAccessResponse } from "@workspace/api"
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
 * lasts a few minutes (`getLink` asks for it: the specialist's own endpoint,
 * or the reviewer's). The tab opens on the click itself, so pop-up blockers
 * allow it, and is pointed at the link once it arrives.
 */
export function useDocumentLink(
  getLink: (() => Promise<DocumentAccessResponse>) | undefined
) {
  const [opening, setOpening] = React.useState(false)
  const [failed, setFailed] = React.useState(false)

  async function open() {
    if (!getLink) return
    const tab = window.open("", "_blank")
    setOpening(true)
    setFailed(false)
    try {
      const { url } = await getLink()
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
  }

  return { open, opening, failed }
}
