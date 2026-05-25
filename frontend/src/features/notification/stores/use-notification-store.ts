import { create } from 'zustand'

import type { NotificationListResponse } from '@/features/notification/model/response/NotificationListResponse'
import type { NotificationSummary } from '@/features/notification/model/response/NotificationSummary'
import { notificationIdValue } from '@/features/notification/lib/notification-parsers'
import type { NotificationId } from '@/features/notification/model/NotificationId'

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
  beginNotificationsLoad: () => void
  replaceNotifications: (response: NotificationListResponse) => void
  failNotificationsLoad: (message: string) => void
  replaceUnreadCount: (unreadCount: number) => void
  finishUnreadCountLoad: () => void
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
  beginNotificationsLoad: () => set((state) => ({ ...state, isLoadingList: true, listError: '' })),
  replaceNotifications: (response) =>
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
    }),
  failNotificationsLoad: (message) =>
    set((state) => ({
      ...state,
      isLoadingList: false,
      listError: message,
      hasLoadedList: true,
    })),
  replaceUnreadCount: (unreadCount) =>
    set((state) => ({
      ...state,
      unreadCount,
      hasLoadedUnreadCount: true,
    })),
  finishUnreadCountLoad: () =>
    set((state) => ({
      ...state,
      hasLoadedUnreadCount: true,
    })),
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
