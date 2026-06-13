import { useCallback, useEffect, useState } from 'react'

import { AddMessageBlock } from '@/apis/message/AddMessageBlock'
import { ListMessageBlocks } from '@/apis/message/ListMessageBlocks'
import { RemoveMessageBlock } from '@/apis/message/RemoveMessageBlock'
import type { MessageBlockEntry } from '@/objects/message/response/MessageBlockEntry'
import { parseUserSearchQuery } from '@/objects/user/request/UserSearchQuery'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { Username } from '@/objects/user/Username'
import { ListUserSuggestions } from '@/apis/user/ListUserSuggestions'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 用户屏蔽列表 hook 输入，包含目标用户、权限和失败兜底文案。
 */
type UseUserSettingsBlockListArgs = {
  hash: string
  isEnabled: boolean
  viewerUsername: Username
}

/**
 * 用户屏蔽列表 hook；加载、添加和移除被屏蔽用户，并维护本地反馈状态。
 */
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

    void sendAPI(new ListMessageBlocks())
      .then((entries) => {
        setBlockedUsers(entries)
        setBlockErrorMessage('')
      })
      .catch((error) => {
        setBlockErrorMessage(isHttpClientError(error) ? error.message : t('messages.blockLoadFailed'))
      })
  }, [isEnabled, t])

  useEffect(() => {
    if (!isEnabled || !blockSearch.trim()) {
      return
    }

    const timeoutId = window.setTimeout(() => {
      const parsedQuery = parseUserSearchQuery(blockSearch)
      if (!parsedQuery.ok) {
        setBlockSuggestions([])
        return
      }

      void sendAPI(new ListUserSuggestions(parsedQuery.value))
        .then((items) => {
          setBlockSuggestions(items.filter((item) => item.username !== viewerUsername))
        })
        .catch((error) => {
          setBlockSuggestions([])
          setBlockErrorMessage(isHttpClientError(error) ? error.message : t('messages.blockActionFailed'))
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
        const entry = await sendAPI(new AddMessageBlock(username))
        setBlockedUsers((current) => [
          entry,
          ...current.filter((item) => item.user.username !== entry.user.username),
        ])
        setBlockSearch('')
        setBlockSuggestions([])
        setBlockErrorMessage('')
      } catch (error) {
        setBlockErrorMessage(isHttpClientError(error) ? error.message : t('messages.blockActionFailed'))
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
        await sendAPI(new RemoveMessageBlock(username))
        setBlockedUsers((current) => current.filter((item) => item.user.username !== username))
        setBlockErrorMessage('')
      } catch (error) {
        setBlockErrorMessage(isHttpClientError(error) ? error.message : t('messages.blockActionFailed'))
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
