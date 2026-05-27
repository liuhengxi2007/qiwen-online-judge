import { useEffect, useState } from 'react'

import { listUserSuggestions } from '@/apis/user/ListUserSuggestions'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import { HttpClientError } from '@/system/api/http-client'

export function useMessageRecipientSuggestions(searchQuery: string, searchFailedMessage: string) {
  const [suggestions, setSuggestions] = useState<UserIdentity[]>([])
  const [searchError, setSearchError] = useState('')

  useEffect(() => {
    if (!searchQuery.trim()) {
      return
    }

    let cancelled = false
    const timeoutId = window.setTimeout(() => {
      void listUserSuggestions(searchQuery)
        .then((items) => {
          if (cancelled) {
            return
          }

          setSuggestions(items)
          setSearchError('')
        })
        .catch((error) => {
          if (cancelled) {
            return
          }

          setSuggestions([])
          setSearchError(error instanceof HttpClientError ? error.message : searchFailedMessage)
        })
    }, 150)

    return () => {
      cancelled = true
      window.clearTimeout(timeoutId)
    }
  }, [searchFailedMessage, searchQuery])

  return {
    suggestions,
    searchError,
  }
}
