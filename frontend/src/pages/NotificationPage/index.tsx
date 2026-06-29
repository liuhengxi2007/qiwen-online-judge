import { useEffect } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom'

import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { useNotificationActions } from './hooks/useNotificationActions'
import { useNotificationStore } from '@/pages/stores/notification/UseNotificationStore'
import { PageShell } from '@/pages/components/PageShell'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'
import { calculateTotalPages, parsePositivePage } from '@/pages/objects/Pagination'
import { usePageSearchParamCorrection } from '@/pages/hooks/usePageSearchParamCorrection'
import { useNotificationRefresh } from './hooks/useNotificationRefresh'
import { NotificationListCard } from './components/NotificationListCard'

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
      <NotificationListCard
        actionError={notificationActions.actionError}
        currentPage={currentPage}
        displayMode={session.preferences.displayMode}
        isLoadingList={isLoadingList}
        isMarkingAllRead={notificationActions.isMarkingAllRead}
        listError={listError}
        notifications={notifications}
        onMarkAllRead={() => void notificationActions.markAllRead({ page: currentPage, pageSize: notificationsPerPage })}
        onOpenNotification={(notification) => {
          void notificationActions.markReadIfNeeded(notification.id, notification.isRead).then(() => {
            navigate(notification.targetAnchor ? `${notification.targetPath}#${notification.targetAnchor}` : notification.targetPath)
          })
        }}
        onPageChange={onPageChange}
        totalPages={totalPages}
        unreadCount={unreadCount}
      />
    </PageShell>
  )
}
