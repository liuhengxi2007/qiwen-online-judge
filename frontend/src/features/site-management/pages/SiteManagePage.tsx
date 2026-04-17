import { Navigate } from 'react-router-dom'

import { SiteManageJudgersCard } from '@/features/site-management/components/site-manage-judgers-card'
import { SiteManageUserCard } from '@/features/site-management/components/site-manage-user-card'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useSiteManageModel } from '@/features/site-management/hooks/use-site-manage-model'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { useI18n } from '@/shared/i18n/i18n'
import { SignedInUser } from '@/shared/components/signed-in-user'

export function SiteManagePage() {
  const { t } = useI18n()
  usePageTitle(t('siteManage.pageTitle'))
  const { session: user, siteManagerSession, navigationIntent: guardNavigationIntent } =
    useSessionGuard({ requireSiteManager: true })
  const {
    users,
    judgers,
    userListError,
    judgerListError,
    statusMessage,
    isLoadingUsers,
    isLoadingJudgers,
    updatingUsername,
    deletingUsername,
    navigationIntent: modelNavigationIntent,
    savePermissions,
    deleteUser,
  } = useSiteManageModel(Boolean(siteManagerSession))

  if (guardNavigationIntent) {
    return <Navigate replace={guardNavigationIntent.replace} to={guardNavigationIntent.to} />
  }

  if (modelNavigationIntent) {
    return <Navigate replace={modelNavigationIntent.replace} to={modelNavigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#fffaf4_0%,#f4efe5_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-stone-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-stone-950">
              {t('siteManage.heading')}
            </h1>
            <SignedInUser className="text-sm text-stone-600" user={user} />
          </div>

          <AncestorNavigation buttonClassName="rounded-full border-stone-300 bg-white" />
        </div>

        <SiteManageUserCard
          model={{
            users,
            judgers,
            userListError,
            judgerListError,
            statusMessage,
            isLoadingUsers,
            isLoadingJudgers,
            updatingUsername,
            deletingUsername,
            navigationIntent: modelNavigationIntent,
            savePermissions,
            deleteUser,
          }}
          siteManagerSession={Boolean(siteManagerSession)}
        />
        <SiteManageJudgersCard
          model={{
            users,
            judgers,
            userListError,
            judgerListError,
            statusMessage,
            isLoadingUsers,
            isLoadingJudgers,
            updatingUsername,
            deletingUsername,
            navigationIntent: modelNavigationIntent,
            savePermissions,
            deleteUser,
          }}
        />
      </section>
    </main>
  )
}
