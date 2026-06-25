import { Bell } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { NotificationSummary } from '@/objects/notification/response/NotificationSummary'
import type { UserDisplayMode } from '@/objects/user/UserDisplayMode'
import { PaginationControls } from '@/pages/components/PaginationControls'
import { useI18n } from '@/system/i18n/use-i18n'

import { NotificationRow } from './NotificationRow'

type NotificationListCardProps = {
  actionError: string
  currentPage: number
  displayMode: UserDisplayMode
  isLoadingList: boolean
  isMarkingAllRead: boolean
  listError: string
  notifications: NotificationSummary[]
  onMarkAllRead: () => void
  onOpenNotification: (notification: NotificationSummary) => void
  onPageChange: (page: number) => void
  totalPages: number
  unreadCount: number
}

/**
 * 通知列表卡片，展示已读操作、列表状态、通知行和分页。
 */
export function NotificationListCard({
  actionError,
  currentPage,
  displayMode,
  isLoadingList,
  isMarkingAllRead,
  listError,
  notifications,
  onMarkAllRead,
  onOpenNotification,
  onPageChange,
  totalPages,
  unreadCount,
}: NotificationListCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
              <Bell className="size-5" />
            </div>
            <div>
              <CardTitle className="text-xl text-slate-950">{t('notifications.listTitle')}</CardTitle>
              <CardDescription>{t('notifications.listDescription')}</CardDescription>
            </div>
          </div>
          <Button
            type="button"
            variant="outline"
            disabled={unreadCount === 0 || isMarkingAllRead}
            className="rounded-2xl border-slate-300 bg-white"
            onClick={onMarkAllRead}
          >
            {isMarkingAllRead ? t('notifications.markingRead') : t('notifications.markAllRead')}
          </Button>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {actionError ? (
          <Alert variant="destructive">
            <AlertDescription>{actionError}</AlertDescription>
          </Alert>
        ) : null}
        {listError ? (
          <Alert variant="destructive">
            <AlertDescription>{listError}</AlertDescription>
          </Alert>
        ) : null}
        {isLoadingList && notifications.length === 0 ? <p className="text-sm text-slate-500">{t('common.loading')}</p> : null}
        {!isLoadingList && notifications.length === 0 ? (
          <p className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-5 py-6 text-sm text-slate-500">
            {t('notifications.empty')}
          </p>
        ) : null}
        <div className="space-y-3">
          {notifications.map((notification) => (
            <NotificationRow
              key={notification.id}
              displayMode={displayMode}
              notification={notification}
              onOpen={() => onOpenNotification(notification)}
            />
          ))}
        </div>
        {!isLoadingList && notifications.length > 0 && totalPages > 1 ? (
          <PaginationControls
            currentPage={currentPage}
            totalPages={totalPages}
            previousLabel={t('common.pagination.previous')}
            nextLabel={t('common.pagination.next')}
            onPageChange={onPageChange}
          />
        ) : null}
      </CardContent>
    </Card>
  )
}
