import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import type { BlogId } from '@/objects/blog/BlogId'
import type { BlogTitle } from '@/objects/blog/BlogTitle'

type BlogReplyNotificationPayload = {
  kind: 'blog_reply'
  blogId: BlogId
  blogTitle: BlogTitle
  triggerCommentId: BlogCommentId
  recipientCommentId: BlogCommentId | null
  contentPreview: string
}

export type NotificationPayload = BlogReplyNotificationPayload