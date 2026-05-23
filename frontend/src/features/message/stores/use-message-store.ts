import { create } from 'zustand'

import type { MessageInboxResponse } from '@/features/message/http/response/MessageInboxResponse'
import type { MessageConversationSummary } from '@/features/message/http/response/MessageConversationSummary'

type MessageStoreState = {
  conversations: MessageConversationSummary[]
  totalUnreadCount: number
  page: number
  pageSize: number
  totalItems: number
  isLoadingInbox: boolean
  inboxError: string
  hasLoadedInbox: boolean
  beginInboxLoad: () => void
  replaceInbox: (inbox: MessageInboxResponse) => void
  failInboxLoad: (message: string) => void
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
  beginInboxLoad: () => set((state) => ({ ...state, isLoadingInbox: true, inboxError: '' })),
  replaceInbox: (inbox) =>
    set({
      conversations: inbox.conversations,
      totalUnreadCount: inbox.totalUnreadCount,
      page: inbox.page,
      pageSize: inbox.pageSize,
      totalItems: inbox.totalItems,
      isLoadingInbox: false,
      inboxError: '',
      hasLoadedInbox: true,
    }),
  failInboxLoad: (message) =>
    set((state) => ({
      ...state,
      isLoadingInbox: false,
      inboxError: message,
      hasLoadedInbox: true,
    })),
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
