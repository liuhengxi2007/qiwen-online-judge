import type { UserIdentity } from '@/objects/user/UserIdentity'
import { fromUserIdentityContract } from '@/objects/user/UserIdentity'
import type { NotificationId } from '@/objects/notification/NotificationId'
import { fromNotificationIdContract } from '@/objects/notification/NotificationId'
import type { NotificationKind } from '@/objects/notification/NotificationKind'
import { fromNotificationKindContract } from '@/objects/notification/NotificationKind'
import type { NotificationPayload } from '@/objects/notification/NotificationPayload'
import { fromNotificationPayloadContract } from '@/objects/notification/NotificationPayload'
import { readBoolean, readNullable, readRecord, readString } from '@/objects/shared/PageResponse'

export type NotificationSummary = {
  id: NotificationId
  kind: NotificationKind
  actor: UserIdentity | null
  titleKey: string
  bodyKey: string
  payload: NotificationPayload
  targetPath: string
  targetAnchor: string | null
  isRead: boolean
  createdAt: string
}

export function fromNotificationSummaryContract(value: unknown, label: string): NotificationSummary {
  const notification = readRecord(value, label)
  return {
    id: fromNotificationIdContract(readString(notification.id, `${label} id`), `${label} id`),
    kind: fromNotificationKindContract(notification.kind),
    actor: readNullable(notification.actor, `${label} actor`, fromUserIdentityContract),
    titleKey: readString(notification.titleKey, `${label} title key`),
    bodyKey: readString(notification.bodyKey, `${label} body key`),
    payload: fromNotificationPayloadContract(notification.payload),
    targetPath: readString(notification.targetPath, `${label} target path`),
    targetAnchor: readNullable(notification.targetAnchor, `${label} target anchor`, readString),
    isRead: readBoolean(notification.isRead, `${label} is read`),
    createdAt: readString(notification.createdAt, `${label} created at`),
  }
}
