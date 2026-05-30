import { blogTitleValue } from '@/objects/blog/BlogTitle'
import type { NotificationPayload } from '@/objects/notification/NotificationPayload'

export function notificationTranslationValues(
  payload: NotificationPayload,
  actorDisplayName: string | null,
): Record<string, string> {
  switch (payload.kind) {
    case 'blog_reply':
      return {
        actor: actorDisplayName ?? '',
        blogTitle: blogTitleValue(payload.blogTitle),
        contentPreview: payload.contentPreview,
      }
  }
}
