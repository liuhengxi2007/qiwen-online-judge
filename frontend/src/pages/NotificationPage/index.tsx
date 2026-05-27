import { useEffect } from 'react'
import { Bell, ChevronRight } from 'lucide-react'
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useSessionGuard } from '@/pages/hooks/use-session-guard'
import { useNotificationActions } from './hooks/use-notification-actions'
import { useNotificationRefresh } from '@/pages/hooks/use-notification-refresh'
import { notificationTranslationValues } from '@/objects/notification/notification-parsers'
import { useNotificationStore } from '@/pages/stores/notification/use-notification-store'
import { AncestorNavigation } from '@/pages/components/ancestor-navigation'
import { AppSectionBar } from '@/pages/components/app-section-bar'
import { DateTimeText } from '@/pages/components/date-time-text'
import { formatUserDisplayLabel } from '@/objects/user/user-display-label'
import { usePageTitle } from '@/pages/hooks/use-page-title'
import { useI18n } from '@/system/i18n/use-i18n'
import { buildPageNumbers, calculateTotalPages, parsePositivePage } from '@/objects/shared/pagination'
import { usePageSearchParamCorrection } from '@/pages/hooks/use-page-search-param-correction'

const notificationsPerPage = 10

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
  const pageNumbers = buildPageNumbers(currentPage, totalPages)

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

  async function openNotification(targetPath: string, targetAnchor: string | null, notificationId: Parameters<typeof markReadLocal>[0], isRead: boolean) {
    await notificationActions.markReadIfNeeded(notificationId, isRead)
    navigate(targetAnchor ? `${targetPath}#${targetAnchor}` : targetPath)
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#eff6ff_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('notifications.heading')}</h1>
            <p className="text-sm text-slate-600">{t('notifications.description', { unreadCount: String(unreadCount) })}</p>
          </div>
          <AncestorNavigation />
        </div>

        <AppSectionBar />

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
              <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                <AlertDescription className="text-rose-700">{notificationActions.actionError}</AlertDescription>
              </Alert>
            ) : null}
            {listError ? (
              <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                <AlertDescription className="text-rose-700">{listError}</AlertDescription>
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
                const actorDisplayName = notification.actor ? formatUserDisplayLabel(notification.actor, session.preferences.displayMode) : t('notifications.systemActor')
                return (
                  <button
                    key={notification.id}
                    type="button"
                    className={`flex w-full items-start justify-between gap-4 rounded-3xl border px-5 py-4 text-left transition ${
                      notification.isRead
                        ? 'border-slate-200 bg-slate-50 hover:border-slate-300 hover:bg-white'
                        : 'border-amber-200 bg-amber-50 hover:border-amber-300 hover:bg-amber-100/60'
                    }`}
                    onClick={() => void openNotification(notification.targetPath, notification.targetAnchor, notification.id, notification.isRead)}
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
                  </button>
                )
              })}
            </div>
            {!isLoadingList && notifications.length > 0 && totalPages > 1 ? (
              <div className="flex flex-wrap items-center justify-center gap-2 pt-4">
                <Button type="button" variant="outline" className="rounded-2xl border-slate-300 bg-white" disabled={currentPage === 1} onClick={() => {
                  const nextSearchParams = new URLSearchParams(searchParams)
                  nextSearchParams.set('page', String(Math.max(1, currentPage - 1)))
                  setSearchParams(nextSearchParams)
                }}>{t('common.pagination.previous')}</Button>
                {pageNumbers.map((page) => (
                  <Button key={page} type="button" variant={page === currentPage ? 'default' : 'outline'} className={page === currentPage ? 'rounded-2xl bg-slate-950 text-white' : 'rounded-2xl border-slate-300 bg-white'} onClick={() => {
                    const nextSearchParams = new URLSearchParams(searchParams)
                    nextSearchParams.set('page', String(page))
                    setSearchParams(nextSearchParams)
                  }}>{page}</Button>
                ))}
                <Button type="button" variant="outline" className="rounded-2xl border-slate-300 bg-white" disabled={currentPage === totalPages} onClick={() => {
                  const nextSearchParams = new URLSearchParams(searchParams)
                  nextSearchParams.set('page', String(Math.min(totalPages, currentPage + 1)))
                  setSearchParams(nextSearchParams)
                }}>{t('common.pagination.next')}</Button>
              </div>
            ) : null}
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
