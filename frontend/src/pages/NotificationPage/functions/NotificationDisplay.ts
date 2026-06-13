import { blogTitleValue } from '@/objects/blog/BlogTitle'
import type { NotificationPayload } from '@/objects/notification/NotificationPayload'

/**
 * 将通知 payload 转换为翻译模板参数，供通知列表展示本地化文案。
 */
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
