export type NotificationUnreadCountResponse = {
  unreadCount: number
}

export function fromNotificationUnreadCountResponseContract(
  value: unknown,
  label = 'notification unread count response',
): NotificationUnreadCountResponse {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new Error(`Invalid ${label} in contract payload: expected object.`)
  }

  const response = value as Record<string, unknown>
  if (
    typeof response.unreadCount !== 'number' ||
    !Number.isSafeInteger(response.unreadCount) ||
    response.unreadCount < 0
  ) {
    throw new Error(`Invalid ${label} unread count in contract payload: expected non-negative safe integer.`)
  }

  return { unreadCount: response.unreadCount }
}
