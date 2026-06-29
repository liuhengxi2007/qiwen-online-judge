/** 通知 UUID 品牌类型；用于标记已读等通知操作。 */
export type NotificationId = string & { readonly __brand: 'NotificationId' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

/** 创建通知 ID 品牌值；调用前必须完成 UUID 校验。 */
function createNotificationId(value: string): NotificationId {
  /** 注意：这里的 as 只在 parseNotificationId 校验通过后施加品牌类型。 */
  return value as NotificationId
}

/** 解析通知 ID；接受 UUID 字符串并返回结构化错误。 */
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

/** 将通知 ID 品牌值还原为字符串；用于 API path。 */
export function notificationIdValue(notificationId: NotificationId): string {
  return notificationId
}
