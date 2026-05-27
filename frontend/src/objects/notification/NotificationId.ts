export type NotificationId = string & { readonly __brand: 'NotificationId' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function createNotificationId(value: string): NotificationId {
  return value as NotificationId
}

export function parseNotificationId(rawId: string): ParseResult<NotificationId> {
  const normalized = rawId.trim()
  if (!normalized) {
    return { ok: false, error: 'Notification id is required.' }
  }
  if (!uuidPattern.test(normalized)) {
    return { ok: false, error: 'Notification id must be a valid UUID.' }
  }

  return { ok: true, value: createNotificationId(normalized) }
}

export function notificationIdValue(notificationId: NotificationId): string {
  return notificationId
}

export function fromNotificationIdContract(value: string, label: string): NotificationId {
  const result = parseNotificationId(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}
