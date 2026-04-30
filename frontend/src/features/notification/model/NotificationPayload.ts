import type { BlogCommentId, BlogId, BlogTitle } from '@/features/blog/domain/blog'

export type BlogReplyNotificationPayload = {
  kind: 'blog_reply'
  blogId: BlogId
  blogTitle: BlogTitle
  triggerCommentId: BlogCommentId
  recipientCommentId: BlogCommentId | null
  contentPreview: string
}

export type NotificationPayload = BlogReplyNotificationPayload
