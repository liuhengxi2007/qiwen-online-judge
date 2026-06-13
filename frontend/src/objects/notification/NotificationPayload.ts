import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import type { BlogId } from '@/objects/blog/BlogId'
import type { BlogTitle } from '@/objects/blog/BlogTitle'

/** 博客回复通知载荷；包含目标博客、触发评论和内容预览。 */
type BlogReplyNotificationPayload = {
  kind: 'blog_reply'
  blogId: BlogId
  blogTitle: BlogTitle
  triggerCommentId: BlogCommentId
  recipientCommentId: BlogCommentId | null
  contentPreview: string
}

/** 通知载荷联合类型；由 kind 决定具体结构。 */
export type NotificationPayload = BlogReplyNotificationPayload
