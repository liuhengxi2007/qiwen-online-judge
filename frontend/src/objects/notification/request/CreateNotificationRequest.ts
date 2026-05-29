import type { NotificationKind } from '@/objects/notification/NotificationKind'
import type { NotificationPayload } from '@/objects/notification/NotificationPayload'
import type { Username } from '@/objects/user/Username'

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
