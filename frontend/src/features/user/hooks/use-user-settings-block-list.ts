import { useCallback, useEffect, useState } from 'react'

import { addMessageBlock } from '@/features/message/http/api/AddMessageBlock'
import { listMessageBlocks } from '@/features/message/http/api/ListMessageBlocks'
import { removeMessageBlock } from '@/features/message/http/api/RemoveMessageBlock'
import type { MessageBlockEntry } from '@/features/message/model/response/MessageBlockEntry'
import type { UserIdentity } from '@/features/user/model/UserIdentity'
import type { Username } from '@/features/user/model/Username'
import { listUserSuggestions } from '@/features/user/http/api/ListUserSuggestions'
import { HttpClientError } from '@/shared/api/http-client'
import { useI18n } from '@/shared/i18n/use-i18n'

type UseUserSettingsBlockListArgs = {
  hash: string
  isEnabled: boolean
  viewerUsername: Username
}

export function useUserSettingsBlockList({
  hash,
  isEnabled,
  viewerUsername,
}: UseUserSettingsBlockListArgs) {
  const { t } = useI18n()
  const [blockSuggestions, setBlockSuggestions] = useState<UserIdentity[]>([])
  const [blockSearch, setBlockSearch] = useState('')
  const [blockErrorMessage, setBlockErrorMessage] = useState('')
  const [isUpdatingBlocks, setIsUpdatingBlocks] = useState(false)
  const [blockedUsers, setBlockedUsers] = useState<MessageBlockEntry[]>([])

  useEffect(() => {
    if (!isEnabled) {
      return
    }

    void listMessageBlocks()
      .then((entries) => {
        setBlockedUsers(entries)
        setBlockErrorMessage('')
      })
      .catch((error) => {
        setBlockErrorMessage(error instanceof HttpClientError ? error.message : t('messages.blockLoadFailed'))
      })
  }, [isEnabled, t])

  useEffect(() => {
    if (!isEnabled || !blockSearch.trim()) {
      return
    }

    const timeoutId = window.setTimeout(() => {
      void listUserSuggestions(blockSearch)
        .then((items) => {
          setBlockSuggestions(items.filter((item) => item.username !== viewerUsername))
        })
        .catch((error) => {
          setBlockSuggestions([])
          setBlockErrorMessage(error instanceof HttpClientError ? error.message : t('messages.blockActionFailed'))
        })
    }, 150)

    return () => window.clearTimeout(timeoutId)
  }, [blockSearch, isEnabled, t, viewerUsername])

  useEffect(() => {
    if (!isEnabled || hash !== '#message-blocks') {
      return
    }

    const frameId = window.requestAnimationFrame(() => {
      document.getElementById('message-blocks')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })

    return () => window.cancelAnimationFrame(frameId)
  }, [hash, isEnabled])

  const addBlock = useCallback(
    async (username: Username) => {
      setIsUpdatingBlocks(true)
      try {
        const entry = await addMessageBlock(username)
        setBlockedUsers((current) => [
          entry,
          ...current.filter((item) => item.user.username !== entry.user.username),
        ])
        setBlockSearch('')
        setBlockSuggestions([])
        setBlockErrorMessage('')
      } catch (error) {
        setBlockErrorMessage(error instanceof HttpClientError ? error.message : t('messages.blockActionFailed'))
      } finally {
        setIsUpdatingBlocks(false)
      }
    },
    [t],
  )

  const removeBlock = useCallback(
    async (username: Username) => {
      setIsUpdatingBlocks(true)
      try {
        await removeMessageBlock(username)
        setBlockedUsers((current) => current.filter((item) => item.user.username !== username))
        setBlockErrorMessage('')
      } catch (error) {
        setBlockErrorMessage(error instanceof HttpClientError ? error.message : t('messages.blockActionFailed'))
      } finally {
        setIsUpdatingBlocks(false)
      }
    },
    [t],
  )

  return {
    blockedUsers,
    blockErrorMessage,
    blockSearch,
    isUpdatingBlocks,
    setBlockSearch,
    visibleBlockSuggestions: isEnabled && blockSearch.trim() ? blockSuggestions : [],
    addBlock,
    removeBlock,
  }
}
