import { create } from 'zustand'

import type { NotificationListResponse } from '@/objects/notification/response/NotificationListResponse'
import type { NotificationSummary } from '@/objects/notification/response/NotificationSummary'
import { notificationIdValue } from '@/objects/notification/NotificationId'
import type { NotificationId } from '@/objects/notification/NotificationId'

/**
 * 通知全局状态，保存列表页数据、未读数、加载标记和本地已读修正动作。
 */
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

/**
 * 通知 Zustand store；由列表查询、未读数查询和实时通知连接共同更新。
 */
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
