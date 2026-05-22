import type { UserIdentity } from '@/features/user/domain/user'
import type { NotificationId } from '@/features/notification/model/NotificationId'
import type { NotificationKind } from '@/features/notification/model/NotificationKind'
import type { NotificationPayload } from '@/features/notification/model/NotificationPayload'

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
