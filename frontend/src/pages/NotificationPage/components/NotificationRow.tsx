import { ChevronRight } from 'lucide-react'

import { RowAction } from '@/components/ui/row-action'
import type { NotificationSummary } from '@/objects/notification/response/NotificationSummary'
import type { UserDisplayMode } from '@/objects/user/UserDisplayMode'
import { DateTimeText } from '@/pages/components/DateTimeText'
import { formatUserDisplayLabel } from '@/pages/objects/UserDisplayLabel'
import { useI18n } from '@/system/i18n/use-i18n'

import { notificationTranslationValues } from '../functions/NotificationDisplay'

type NotificationRowProps = {
  displayMode: UserDisplayMode
  notification: NotificationSummary
  onOpen: () => void
}

/**
 * 单条通知行，负责未读标记、本地化文案和点击入口。
 */
export function NotificationRow({ displayMode, notification, onOpen }: NotificationRowProps) {
  const { t } = useI18n()
  const actorDisplayName = notification.actor
    ? formatUserDisplayLabel(notification.actor, displayMode)
    : t('notifications.systemActor')

  return (
    <RowAction
      size="spacious"
      variant={notification.isRead ? 'default' : 'warning'}
      onClick={onOpen}
    >
      <div className="space-y-2">
        <div className="flex flex-wrap items-center gap-2">
          {!notification.isRead ? (
            <span className="rounded-full bg-amber-300 px-2.5 py-1 text-xs font-semibold text-amber-950">
              {t('notifications.unread')}
            </span>
          ) : null}
          <DateTimeText className="text-xs uppercase tracking-[0.16em] text-slate-500" value={notification.createdAt} />
        </div>
        <p className="text-base font-semibold text-slate-950">
          {t(notification.titleKey, notificationTranslationValues(notification.payload, actorDisplayName))}
        </p>
        <p className="text-sm leading-6 text-slate-600">
          {t(notification.bodyKey, notificationTranslationValues(notification.payload, actorDisplayName))}
        </p>
      </div>
      <ChevronRight className="mt-1 size-4 shrink-0 text-slate-400" />
    </RowAction>
  )
}
