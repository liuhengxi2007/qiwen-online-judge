import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import { fromBlogCommentIdContract } from '@/objects/blog/BlogCommentId'
import type { BlogId } from '@/objects/blog/BlogId'
import { fromBlogIdContract } from '@/objects/blog/BlogId'
import type { BlogTitle } from '@/objects/blog/BlogTitle'
import { fromBlogTitleContract } from '@/objects/blog/BlogTitle'

type BlogReplyNotificationPayload = {
  kind: 'blog_reply'
  blogId: BlogId
  blogTitle: BlogTitle
  triggerCommentId: BlogCommentId
  recipientCommentId: BlogCommentId | null
  contentPreview: string
}

export type NotificationPayload = BlogReplyNotificationPayload

export function fromNotificationPayloadContract(payload: unknown): NotificationPayload {
  if (!isRecord(payload) || typeof payload.kind !== 'string') {
    throw new Error('Invalid notification payload.')
  }

  switch (payload.kind) {
    case 'blog_reply':
      return fromBlogReplyNotificationPayloadContract(payload)
    default:
      throw new Error('Invalid notification payload kind.')
  }
}

function fromBlogReplyNotificationPayloadContract(payload: unknown): BlogReplyNotificationPayload {
  if (!isRecord(payload)) {
    throw new Error('Invalid notification payload.')
  }

  return {
    kind: 'blog_reply',
    blogId: fromBlogIdContract(readNumber(payload.blogId, 'notification blog id'), 'notification blog id'),
    blogTitle: fromBlogTitleContract(readString(payload.blogTitle, 'notification blog title'), 'notification blog title'),
    triggerCommentId: fromBlogCommentIdContract(
      readNumber(payload.triggerCommentId, 'notification trigger comment id'),
      'notification trigger comment id',
    ),
    recipientCommentId:
      payload.recipientCommentId === null
        ? null
        : fromBlogCommentIdContract(
            readNumber(payload.recipientCommentId, 'notification recipient comment id'),
            'notification recipient comment id',
          ),
    contentPreview: readString(payload.contentPreview, 'notification content preview'),
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
