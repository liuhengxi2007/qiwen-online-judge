import { fromUserIdentityContract } from '@/features/auth/domain/auth-contract'
import { requireParsed } from '@/features/auth/domain/auth-parsers'
import {
  blogTitleValue,
  parseBlogCommentId,
  parseBlogId,
  parseBlogTitle,
} from '@/features/blog/domain/blog'
import type { NotificationListResponse } from '@/features/notification/http/response/NotificationListResponse'
import type { NotificationId } from '@/features/notification/model/NotificationId'
import type { NotificationKind } from '@/features/notification/model/NotificationKind'
import type { NotificationPayload } from '@/features/notification/model/NotificationPayload'
import type { NotificationSummary } from '@/features/notification/http/response/NotificationSummary'
import type { NotificationUnreadCountResponse } from '@/features/notification/http/response/NotificationUnreadCountResponse'

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function createNotificationId(value: string): NotificationId {
  return value as NotificationId
}

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

function readNotificationKind(value: unknown): NotificationKind {
  const kind = readString(value, 'notification kind')
  if (kind !== 'blog_reply') {
    throw new Error('Invalid notification kind.')
  }

  return kind
}

export function parseNotificationId(rawId: string) {
  const normalized = rawId.trim()
  if (!normalized) {
    return { ok: false as const, error: 'Notification id is required.' }
  }
  if (!uuidPattern.test(normalized)) {
    return { ok: false as const, error: 'Notification id must be a valid UUID.' }
  }

  return { ok: true as const, value: createNotificationId(normalized) }
}

export function notificationIdValue(notificationId: NotificationId): string {
  return notificationId
}

function fromBlogReplyPayload(payload: unknown) {
  if (!isRecord(payload)) {
    throw new Error('Invalid notification payload.')
  }

  return {
    kind: 'blog_reply' as const,
    blogId: requireParsed(parseBlogId(readNumber(payload.blogId, 'notification blog id')), 'notification blog id'),
    blogTitle: requireParsed(parseBlogTitle(readString(payload.blogTitle, 'notification blog title')), 'notification blog title'),
    triggerCommentId: requireParsed(parseBlogCommentId(readNumber(payload.triggerCommentId, 'notification trigger comment id')), 'notification trigger comment id'),
    recipientCommentId:
      payload.recipientCommentId === null
        ? null
        : requireParsed(parseBlogCommentId(readNumber(payload.recipientCommentId, 'notification recipient comment id')), 'notification recipient comment id'),
    contentPreview: readString(payload.contentPreview, 'notification content preview'),
  }
}

export function fromNotificationPayloadContract(payload: unknown): NotificationPayload {
  if (!isRecord(payload) || typeof payload.kind !== 'string') {
    throw new Error('Invalid notification payload.')
  }

  switch (payload.kind) {
    case 'blog_reply':
      return fromBlogReplyPayload(payload)
    default:
      throw new Error('Invalid notification payload kind.')
  }
}

export function fromNotificationSummaryContract(notification: unknown): NotificationSummary {
  if (!isRecord(notification)) {
    throw new Error('Invalid notification payload.')
  }

  return {
    id: requireParsed(parseNotificationId(readString(notification.id, 'notification id')), 'notification id'),
    kind: readNotificationKind(notification.kind),
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

export function notificationTranslationValues(payload: NotificationPayload, actorDisplayName: string | null): Record<string, string> {
  switch (payload.kind) {
    case 'blog_reply':
      return {
        actor: actorDisplayName ?? '',
        blogTitle: blogTitleValue(payload.blogTitle),
        contentPreview: payload.contentPreview,
      }
  }
}
