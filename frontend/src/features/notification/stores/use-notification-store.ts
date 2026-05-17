import { create } from 'zustand'

import { getNotificationUnreadCount, listNotifications } from '@/features/notification/api/notification-client'
import type { NotificationId, NotificationSummary } from '@/features/notification/domain/notification'
import { notificationIdValue } from '@/features/notification/domain/notification'
import { HttpClientError } from '@/shared/api/http-client'
import type { PageRequest } from '@/shared/model/Pagination'

type NotificationStoreState = {
  notifications: NotificationSummary[]
  unreadCount: number
  page: number
  pageSize: number
  totalItems: number
  isLoadingList: boolean
  listError: string
  hasLoadedList: boolean
  hasLoadedUnreadCount: boolean
  refreshNotifications: (pageRequest?: PageRequest) => Promise<void>
  refreshUnreadCount: () => Promise<void>
  markReadLocal: (notificationId: NotificationId) => void
  markAllReadLocal: () => void
  clear: () => void
}

export const useNotificationStore = create<NotificationStoreState>((set) => ({
  notifications: [],
  unreadCount: 0,
  page: 1,
  pageSize: 10,
  totalItems: 0,
  isLoadingList: false,
  listError: '',
  hasLoadedList: false,
  hasLoadedUnreadCount: false,
  refreshNotifications: async (pageRequest) => {
    set((state) => ({ ...state, isLoadingList: true, listError: '' }))
    try {
      const response = await listNotifications(pageRequest)
      set({
        notifications: response.notifications,
        unreadCount: response.unreadCount,
        page: response.page,
        pageSize: response.pageSize,
        totalItems: response.totalItems,
        isLoadingList: false,
        listError: '',
        hasLoadedList: true,
        hasLoadedUnreadCount: true,
      })
    } catch (error) {
      set((state) => ({
        ...state,
        isLoadingList: false,
        listError: error instanceof HttpClientError ? error.message : 'Unable to load notifications.',
        hasLoadedList: true,
      }))
    }
  },
  refreshUnreadCount: async () => {
    try {
      const response = await getNotificationUnreadCount()
      set((state) => ({
        ...state,
        unreadCount: response.unreadCount,
        hasLoadedUnreadCount: true,
      }))
    } catch {
      set((state) => ({
        ...state,
        hasLoadedUnreadCount: true,
      }))
    }
  },
  markReadLocal: (notificationId) =>
    set((state) => {
      const nextNotifications = state.notifications.map((notification) =>
        notificationIdValue(notification.id) === notificationIdValue(notificationId)
          ? { ...notification, isRead: true }
          : notification,
      )
      const wasUnread = state.notifications.some(
        (notification) => notificationIdValue(notification.id) === notificationIdValue(notificationId) && !notification.isRead,
      )

      return {
        ...state,
        notifications: nextNotifications,
        unreadCount: wasUnread ? Math.max(0, state.unreadCount - 1) : state.unreadCount,
      }
    }),
  markAllReadLocal: () =>
    set((state) => ({
      ...state,
      notifications: state.notifications.map((notification) => ({ ...notification, isRead: true })),
      unreadCount: 0,
    })),
  clear: () =>
    set({
      notifications: [],
      unreadCount: 0,
      page: 1,
      pageSize: 10,
      totalItems: 0,
      isLoadingList: false,
      listError: '',
      hasLoadedList: false,
      hasLoadedUnreadCount: false,
    }),
}))
