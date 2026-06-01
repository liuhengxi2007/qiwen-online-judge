import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import { fromBlogCommentIdContract } from '@/objects/blog/BlogCommentId'
import type { BlogId } from '@/objects/blog/BlogId'
import { fromBlogIdContract } from '@/objects/blog/BlogId'
import type { BlogTitle } from '@/objects/blog/BlogTitle'
import { fromBlogTitleContract } from '@/objects/blog/BlogTitle'
import { readNullable, readRecord, readSafeInteger, readString } from '@/objects/shared/PageResponse'

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
  const payloadRecord = readRecord(payload, 'notification payload')
  const kind = readString(payloadRecord.kind, 'notification payload kind')

  switch (kind) {
    case 'blog_reply':
      return fromBlogReplyNotificationPayloadContract(payloadRecord)
    default:
      throw new Error('Invalid notification payload kind.')
  }
}

function fromBlogReplyNotificationPayloadContract(payload: Record<string, unknown>): BlogReplyNotificationPayload {
  return {
    kind: 'blog_reply',
    blogId: fromBlogIdContract(readSafeInteger(payload.blogId, 'notification blog id'), 'notification blog id'),
    blogTitle: fromBlogTitleContract(readString(payload.blogTitle, 'notification blog title'), 'notification blog title'),
    triggerCommentId: fromBlogCommentIdContract(
      readSafeInteger(payload.triggerCommentId, 'notification trigger comment id'),
      'notification trigger comment id',
    ),
    recipientCommentId: readNullable(payload.recipientCommentId, 'notification recipient comment id', (commentId, label) =>
      fromBlogCommentIdContract(readSafeInteger(commentId, label), label),
    ),
    contentPreview: readString(payload.contentPreview, 'notification content preview'),
  }
}
