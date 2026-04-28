import { create } from 'zustand'

import { listInbox } from '@/features/message/api/message-client'
import type { MessageConversationSummary } from '@/features/message/domain/message'
import { HttpClientError } from '@/shared/api/http-client'

type MessageStoreState = {
  conversations: MessageConversationSummary[]
  totalUnreadCount: number
  isLoadingInbox: boolean
  inboxError: string
  hasLoadedInbox: boolean
  refreshInbox: () => Promise<void>
  clear: () => void
}

export const useMessageStore = create<MessageStoreState>((set) => ({
  conversations: [],
  totalUnreadCount: 0,
  isLoadingInbox: false,
  inboxError: '',
  hasLoadedInbox: false,
  refreshInbox: async () => {
    set((state) => ({ ...state, isLoadingInbox: true, inboxError: '' }))
    try {
      const inbox = await listInbox()
      set({
        conversations: inbox.conversations,
        totalUnreadCount: inbox.totalUnreadCount,
        isLoadingInbox: false,
        inboxError: '',
        hasLoadedInbox: true,
      })
    } catch (error) {
      set((state) => ({
        ...state,
        isLoadingInbox: false,
        inboxError: error instanceof HttpClientError ? error.message : 'Unable to load messages.',
        hasLoadedInbox: true,
      }))
    }
  },
  clear: () =>
    set({
      conversations: [],
      totalUnreadCount: 0,
      isLoadingInbox: false,
      inboxError: '',
      hasLoadedInbox: false,
    }),
}))
