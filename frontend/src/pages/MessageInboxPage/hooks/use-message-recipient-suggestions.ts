import { useEffect, useState } from 'react'

import { ListUserSuggestions } from '@/apis/user/ListUserSuggestions'
import { parseUserSearchQuery } from '@/objects/user/request/UserSearchQuery'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import { sendAPI } from '@/system/api/api-message'
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
      const parsedQuery = parseUserSearchQuery(searchQuery)
      if (!parsedQuery.ok) {
        setSuggestions([])
        return
      }

      void sendAPI(new ListUserSuggestions(parsedQuery.value))
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
