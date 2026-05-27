export type NotificationKind = 'blog_reply'

export function fromNotificationKindContract(value: unknown): NotificationKind {
  if (value !== 'blog_reply') {
    throw new Error('Invalid notification kind.')
  }

  return value
}
