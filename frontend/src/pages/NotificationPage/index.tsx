import { useEffect } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { Bell, ChevronRight } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { RowAction } from '@/components/ui/row-action'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { useNotificationActions } from './hooks/useNotificationActions'
import { useNotificationStore } from '@/pages/stores/notification/UseNotificationStore'
import { DateTimeText } from '@/pages/components/DateTimeText'
import { PaginationControls } from '@/pages/components/PaginationControls'
import { PageShell } from '@/pages/components/PageShell'
import { formatUserDisplayLabel } from '@/pages/objects/UserDisplayLabel'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'
import { notificationTranslationValues } from './functions/NotificationDisplay'
import { calculateTotalPages, parsePositivePage } from '@/pages/objects/Pagination'
import { usePageSearchParamCorrection } from '@/pages/hooks/usePageSearchParamCorrection'
import { useNotificationRefresh } from './hooks/useNotificationRefresh'

const notificationsPerPage = 10

/**
 * 通知列表页，负责会话守卫、分页加载、已读操作和实时 store 状态展示。
 */
export function NotificationPage() {
  const { t } = useI18n()
  usePageTitle(t('notifications.pageTitle'))
  const navigate = useNavigate()
  const { session, navigationIntent } = useSessionGuard()
  const notifications = useNotificationStore((state) => state.notifications)
  const unreadCount = useNotificationStore((state) => state.unreadCount)
  const totalItems = useNotificationStore((state) => state.totalItems)
  const pageSize = useNotificationStore((state) => state.pageSize)
  const isLoadingList = useNotificationStore((state) => state.isLoadingList)
  const listError = useNotificationStore((state) => state.listError)
  const { refreshNotifications } = useNotificationRefresh()
  const markReadLocal = useNotificationStore((state) => state.markReadLocal)
  const markAllReadLocal = useNotificationStore((state) => state.markAllReadLocal)
  const notificationActions = useNotificationActions({
    refreshNotifications,
    markReadLocal,
    markAllReadLocal,
    markAllReadFailedMessage: t('notifications.markAllReadFailed'),
  })
  const [searchParams, setSearchParams] = useSearchParams()
  const currentPage = parsePositivePage(searchParams.get('page'))
  const totalPages = calculateTotalPages(totalItems, pageSize)

  useEffect(() => {
    void refreshNotifications({ page: currentPage, pageSize: notificationsPerPage })
  }, [currentPage, refreshNotifications])

  usePageSearchParamCorrection({
    currentPage,
    totalPages,
    isLoading: isLoadingList,
    setSearchParams,
  })

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!session) {
    return <Navigate replace to="/login" />
  }

  const onPageChange = (page: number) => {
    const nextSearchParams = new URLSearchParams(searchParams)
    nextSearchParams.set('page', String(page))
    setSearchParams(nextSearchParams)
  }

  return (
    <PageShell
      title={t('notifications.heading')}
      description={t('notifications.description', { unreadCount: String(unreadCount) })}
      mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#eff6ff_100%)]"
    >
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
              disabled={unreadCount === 0 || notificationActions.isMarkingAllRead}
              className="rounded-2xl border-slate-300 bg-white"
              onClick={() => {
                void notificationActions.markAllRead({ page: currentPage, pageSize: notificationsPerPage })
              }}
            >
              {notificationActions.isMarkingAllRead ? t('notifications.markingRead') : t('notifications.markAllRead')}
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {notificationActions.actionError ? (
            <Alert variant="destructive">
              <AlertDescription>{notificationActions.actionError}</AlertDescription>
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
            {notifications.map((notification) => {
              const actorDisplayName = notification.actor
                ? formatUserDisplayLabel(notification.actor, session.preferences.displayMode)
                : t('notifications.systemActor')
              return (
                <RowAction
                  key={notification.id}
                  size="spacious"
                  variant={notification.isRead ? 'default' : 'warning'}
                  onClick={() => {
                    void notificationActions.markReadIfNeeded(notification.id, notification.isRead).then(() => {
                      navigate(notification.targetAnchor ? `${notification.targetPath}#${notification.targetAnchor}` : notification.targetPath)
                    })
                  }}
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
            })}
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
    </PageShell>
  )
}
