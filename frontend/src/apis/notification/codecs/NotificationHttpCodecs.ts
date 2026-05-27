import type { NotificationListResponse } from '@/objects/notification/response/NotificationListResponse'
import type { NotificationSummary } from '@/objects/notification/response/NotificationSummary'
import type { NotificationUnreadCountResponse } from '@/objects/notification/response/NotificationUnreadCountResponse'
import { fromNotificationIdContract } from '@/objects/notification/NotificationId'
import { fromNotificationKindContract } from '@/objects/notification/NotificationKind'
import { fromNotificationPayloadContract } from '@/objects/notification/NotificationPayload'
import { fromUserIdentityContract } from '@/objects/user/UserIdentity'

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function readNumber(value: unknown, field: string): number {
  if (typeof value !== 'number') {
    throw new Error(`Invalid ${field}.`)
  }

  return value
}

function readString(value: unknown, field: string): string {
  if (typeof value !== 'string') {
    throw new Error(`Invalid ${field}.`)
  }

  return value
}

function readBoolean(value: unknown, field: string): boolean {
  if (typeof value !== 'boolean') {
    throw new Error(`Invalid ${field}.`)
  }

  return value
}

function readUserIdentity(value: unknown, field: string) {
  if (!isRecord(value) || typeof value.username !== 'string' || typeof value.displayName !== 'string') {
    throw new Error(`Invalid ${field}.`)
  }

  return fromUserIdentityContract({
    username: value.username,
    displayName: value.displayName,
  })
}

export function fromNotificationSummaryContract(notification: unknown): NotificationSummary {
  if (!isRecord(notification)) {
    throw new Error('Invalid notification payload.')
  }

  return {
    id: fromNotificationIdContract(readString(notification.id, 'notification id'), 'notification id'),
    kind: fromNotificationKindContract(notification.kind),
    actor: notification.actor === null ? null : readUserIdentity(notification.actor, 'notification actor'),
    titleKey: readString(notification.titleKey, 'notification title key'),
    bodyKey: readString(notification.bodyKey, 'notification body key'),
    payload: fromNotificationPayloadContract(notification.payload),
    targetPath: readString(notification.targetPath, 'notification target path'),
    targetAnchor: notification.targetAnchor === null ? null : readString(notification.targetAnchor, 'notification target anchor'),
    isRead: readBoolean(notification.isRead, 'notification read flag'),
    createdAt: readString(notification.createdAt, 'notification created at'),
  }
}

export function fromNotificationListResponse(value: unknown): NotificationListResponse {
  if (!isRecord(value) || !Array.isArray(value.notifications)) {
    throw new Error('Invalid notification list payload.')
  }

  return {
    notifications: value.notifications.map(fromNotificationSummaryContract),
    unreadCount: readNumber(value.unreadCount, 'notification unread count'),
    page: readNumber(value.page, 'notification page'),
    pageSize: readNumber(value.pageSize, 'notification page size'),
    totalItems: readNumber(value.totalItems, 'notification total items'),
  }
}

export function fromNotificationUnreadCountResponse(value: unknown): NotificationUnreadCountResponse {
  if (!isRecord(value)) {
    throw new Error('Invalid notification unread count payload.')
  }

  return {
    unreadCount: readNumber(value.unreadCount, 'notification unread count'),
  }
}
