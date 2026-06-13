import type { NotificationKind } from '@/objects/notification/NotificationKind'
import type { NotificationPayload } from '@/objects/notification/NotificationPayload'
import type { Username } from '@/objects/user/Username'

/** 内部创建通知请求体；title/body key 和 payload 由调用方按通知类型配套提供。 */
export type CreateNotificationRequest = {
  recipientUsername: Username
  actorUsername: Username | null
  kind: NotificationKind
  titleKey: string
  bodyKey: string
  payload: NotificationPayload
  targetPath: string
  targetAnchor: string | null
}
