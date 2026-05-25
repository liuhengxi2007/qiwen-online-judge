import { useEffect, useState } from 'react'

import { listUserSuggestions } from '@/features/user/http/api/ListUserSuggestions'
import type { UserIdentity } from '@/features/user/model/UserIdentity'
import { HttpClientError } from '@/shared/api/http-client'

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
