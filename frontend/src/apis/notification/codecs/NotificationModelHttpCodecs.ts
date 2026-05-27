import type { NotificationKind } from '@/objects/notification/NotificationKind'
import type { NotificationPayload } from '@/objects/notification/NotificationPayload'
import type { NotificationId } from '@/objects/notification/NotificationId'
import {
  fromBlogCommentIdContract,
  fromBlogIdContract,
  fromBlogTitleContract,
} from '@/apis/blog/codecs/BlogModelHttpCodecs'
import { parseNotificationId } from '@/objects/notification/notification-parsers'
import { requireParsed } from '@/objects/user/user-parsers'

export type NotificationIdContract = string
export type NotificationKindContract = 'blog_reply'

export function fromNotificationIdContract(value: NotificationIdContract, label: string): NotificationId {
  return requireParsed(parseNotificationId(value), label)
}

export function fromNotificationKindContract(value: unknown): NotificationKind {
  const kind = readString(value, 'notification kind')
  if (kind !== 'blog_reply') {
    throw new Error('Invalid notification kind.')
  }

  return kind
}

function fromBlogReplyPayload(payload: unknown) {
  if (!isRecord(payload)) {
    throw new Error('Invalid notification payload.')
  }

  return {
    kind: 'blog_reply' as const,
    blogId: fromBlogIdContract(readNumber(payload.blogId, 'notification blog id'), 'notification blog id'),
    blogTitle: fromBlogTitleContract(readString(payload.blogTitle, 'notification blog title'), 'notification blog title'),
    triggerCommentId: fromBlogCommentIdContract(readNumber(payload.triggerCommentId, 'notification trigger comment id'), 'notification trigger comment id'),
    recipientCommentId:
      payload.recipientCommentId === null
        ? null
        : fromBlogCommentIdContract(readNumber(payload.recipientCommentId, 'notification recipient comment id'), 'notification recipient comment id'),
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
