import { blogTitleValue } from '@/objects/blog/blog-parsers'
import type { NotificationId } from '@/objects/notification/NotificationId'
import type { NotificationPayload } from '@/objects/notification/NotificationPayload'

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function createNotificationId(value: string): NotificationId {
  return value as NotificationId
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

export function notificationTranslationValues(payload: NotificationPayload, actorDisplayName: string | null): Record<string, string> {
  switch (payload.kind) {
    case 'blog_reply':
      return {
        actor: actorDisplayName ?? '',
        blogTitle: blogTitleValue(payload.blogTitle),
        contentPreview: payload.contentPreview,
      }
  }
}
