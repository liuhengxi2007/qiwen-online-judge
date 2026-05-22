import type { BlogCommentId } from '@/features/blog/model/BlogCommentId'
import type { BlogId } from '@/features/blog/model/BlogId'
import type { BlogTitle } from '@/features/blog/model/BlogTitle'

export type BlogReplyNotificationPayload = {
  kind: 'blog_reply'
  blogId: BlogId
  blogTitle: BlogTitle
  triggerCommentId: BlogCommentId
  recipientCommentId: BlogCommentId | null
  contentPreview: string
}
