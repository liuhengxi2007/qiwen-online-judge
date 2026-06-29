import { useEffect, useState } from 'react'

import { ListUserSuggestions } from '@/apis/user/ListUserSuggestions'
import { parseUserSearchQuery } from '@/objects/user/request/UserSearchQuery'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'

/**
 * 私信收件人建议 hook；输入搜索文本，延迟查询用户建议并返回建议列表和错误。
 */
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
          setSearchError(isHttpClientError(error) ? error.message : searchFailedMessage)
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
