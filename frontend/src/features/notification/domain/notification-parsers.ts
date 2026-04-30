import type {
  BlogReplyNotificationPayload as BlogReplyNotificationPayloadContract,
  NotificationPayload as NotificationPayloadContract,
  NotificationSummary as NotificationSummaryContract,
} from '@contracts/notification'
import { fromUserIdentityContract } from '@/features/auth/domain/auth-contract'
import { requireParsed } from '@/features/auth/domain/auth-parsers'
import {
  parseBlogCommentId,
  parseBlogId,
  parseBlogTitle,
} from '@/features/blog/domain/blog'
import type { NotificationListResponse } from '@/features/notification/model/NotificationListResponse'
import type { NotificationId } from '@/features/notification/model/NotificationId'
import type { NotificationPayload } from '@/features/notification/model/NotificationPayload'
import type { NotificationSummary } from '@/features/notification/model/NotificationSummary'
import type { NotificationUnreadCountResponse } from '@/features/notification/model/NotificationUnreadCountResponse'

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function createNotificationId(value: string): NotificationId {
  return value as NotificationId
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null
}

function readNumber(value: unknown, field: string): number {
  if (typeof value !== 'number') {
    throw new Error(`Invalid ${field}.`)
  }

  return value
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

function fromBlogReplyPayload(payload: BlogReplyNotificationPayloadContract) {
  return {
    kind: 'blog_reply' as const,
    blogId: requireParsed(parseBlogId(payload.blogId), 'notification blog id'),
    blogTitle: requireParsed(parseBlogTitle(payload.blogTitle), 'notification blog title'),
    triggerCommentId: requireParsed(parseBlogCommentId(payload.triggerCommentId), 'notification trigger comment id'),
    recipientCommentId:
      payload.recipientCommentId === null
        ? null
        : requireParsed(parseBlogCommentId(payload.recipientCommentId), 'notification recipient comment id'),
    contentPreview: payload.contentPreview,
  }
}

export function fromNotificationPayloadContract(payload: NotificationPayloadContract): NotificationPayload {
  switch (payload.kind) {
    case 'blog_reply':
      return fromBlogReplyPayload(payload)
  }
}

export function fromNotificationSummaryContract(notification: NotificationSummaryContract): NotificationSummary {
  return {
    id: requireParsed(parseNotificationId(notification.id), 'notification id'),
    kind: notification.kind,
    actor: notification.actor === null ? null : fromUserIdentityContract(notification.actor),
    titleKey: notification.titleKey,
    bodyKey: notification.bodyKey,
    payload: fromNotificationPayloadContract(notification.payload),
    targetPath: notification.targetPath,
    targetAnchor: notification.targetAnchor,
    isRead: notification.isRead,
    createdAt: notification.createdAt,
  }
}

export function fromNotificationListResponse(value: unknown): NotificationListResponse {
  if (!isRecord(value) || !Array.isArray(value.notifications)) {
    throw new Error('Invalid notification list payload.')
  }

  return {
    notifications: value.notifications.map((notification) => fromNotificationSummaryContract(notification as NotificationSummaryContract)),
    unreadCount: readNumber(value.unreadCount, 'notification unread count'),
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
        blogTitle: payload.blogTitle as unknown as string,
        contentPreview: payload.contentPreview,
      }
  }
}
