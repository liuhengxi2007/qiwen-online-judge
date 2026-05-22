import { create } from 'zustand'

import { listInbox } from '@/features/message/http/api/message-client'
import type { MessageConversationSummary } from '@/features/message/domain/message'
import { HttpClientError } from '@/shared/api/http-client'
import type { PageRequest } from '@/shared/model/Pagination'

type MessageStoreState = {
  conversations: MessageConversationSummary[]
  totalUnreadCount: number
  page: number
  pageSize: number
  totalItems: number
  isLoadingInbox: boolean
  inboxError: string
  hasLoadedInbox: boolean
  refreshInbox: (pageRequest?: PageRequest) => Promise<void>
  clear: () => void
}

export const useMessageStore = create<MessageStoreState>((set) => ({
  conversations: [],
  totalUnreadCount: 0,
  page: 1,
  pageSize: 10,
  totalItems: 0,
  isLoadingInbox: false,
  inboxError: '',
  hasLoadedInbox: false,
  refreshInbox: async (pageRequest) => {
    set((state) => ({ ...state, isLoadingInbox: true, inboxError: '' }))
    try {
      const inbox = await listInbox(pageRequest)
      set({
        conversations: inbox.conversations,
        totalUnreadCount: inbox.totalUnreadCount,
        page: inbox.page,
        pageSize: inbox.pageSize,
        totalItems: inbox.totalItems,
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
      page: 1,
      pageSize: 10,
      totalItems: 0,
      isLoadingInbox: false,
      inboxError: '',
      hasLoadedInbox: false,
    }),
}))
