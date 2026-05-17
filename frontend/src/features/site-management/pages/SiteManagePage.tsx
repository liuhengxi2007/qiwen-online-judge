import { useEffect, useState } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'

import { SiteManageJudgersCard } from '@/features/site-management/components/site-manage-judgers-card'
import { SiteManageUserCard } from '@/features/site-management/components/site-manage-user-card'
import { AppSectionBar } from '@/shared/components/app-section-bar'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useSiteManageModel } from '@/features/site-management/hooks/use-site-manage-model'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { parseUserSearchQuery } from '@/features/user/domain/user'
import { calculateTotalPages, getPageCorrection, parsePositivePage } from '@/shared/domain/pagination'
import { useI18n } from '@/shared/i18n/use-i18n'

export function SiteManagePage() {
  const { t } = useI18n()
  usePageTitle(t('siteManage.pageTitle'))
  const [searchParams, setSearchParams] = useSearchParams()
  const activeQuery = searchParams.get('q')?.trim() ?? ''
  const [queryInput, setQueryInput] = useState(activeQuery)
  const currentPage = parsePositivePage(searchParams.get('page'))
  const { session: user, siteManagerSession, navigationIntent: guardNavigationIntent } =
    useSessionGuard({ requireSiteManager: true })
  const {
    users,
    judgers,
    userListError,
    judgerListError,
    notice,
    isLoadingUsers,
    isLoadingJudgers,
    userPage,
    userPageSize,
    totalUsers,
    updatingUsername,
    deletingUsername,
    navigationIntent: modelNavigationIntent,
    savePermissions,
    deleteUser,
  } = useSiteManageModel(Boolean(siteManagerSession), {
    query: (() => {
      if (!activeQuery) {
        return null
      }
      const parsedQuery = parseUserSearchQuery(activeQuery)
      return parsedQuery.ok ? parsedQuery.value : null
    })(),
    pageRequest: {
      page: currentPage,
      pageSize: 10,
    },
  })
  const totalPages = calculateTotalPages(totalUsers, userPageSize)

  useEffect(() => {
    setQueryInput(activeQuery)
  }, [activeQuery])

  useEffect(() => {
    if (isLoadingUsers) {
      return
    }

    const correction = getPageCorrection(currentPage, totalPages)
    if (correction.kind === 'none') {
      return
    }

    const nextSearchParams = new URLSearchParams(searchParams)
    if (correction.kind === 'delete') {
      nextSearchParams.delete('page')
    } else {
      nextSearchParams.set('page', String(correction.page))
    }
    setSearchParams(nextSearchParams)
  }, [currentPage, isLoadingUsers, searchParams, setSearchParams, totalPages])

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
          </div>

          <AncestorNavigation buttonClassName="rounded-full border-stone-300 bg-white" />
        </div>

        <AppSectionBar />

        <SiteManageUserCard
          model={{
            users,
            judgers,
            userListError,
            judgerListError,
          notice,
          isLoadingUsers,
          isLoadingJudgers,
          userPage,
          userPageSize,
          totalUsers,
          updatingUsername,
          deletingUsername,
          navigationIntent: modelNavigationIntent,
          savePermissions,
          deleteUser,
        }}
        siteManagerSession={Boolean(siteManagerSession)}
        queryInput={queryInput}
        hasActiveQuery={Boolean(activeQuery)}
        onQueryInputChange={setQueryInput}
        onApplyQuery={() => {
          const nextSearchParams = new URLSearchParams(searchParams)
          if (!queryInput.trim()) {
            nextSearchParams.delete('q')
          } else {
            nextSearchParams.set('q', queryInput.trim())
          }
          nextSearchParams.delete('page')
          setSearchParams(nextSearchParams)
        }}
        onClearQuery={() => {
          setQueryInput('')
          const nextSearchParams = new URLSearchParams(searchParams)
          nextSearchParams.delete('q')
          nextSearchParams.delete('page')
          setSearchParams(nextSearchParams)
        }}
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={(page) => {
          const nextSearchParams = new URLSearchParams(searchParams)
          nextSearchParams.set('page', String(page))
          setSearchParams(nextSearchParams)
        }}
      />
        <SiteManageJudgersCard
          model={{
            users,
            judgers,
            userListError,
            judgerListError,
            notice,
            isLoadingUsers,
            isLoadingJudgers,
            userPage,
            userPageSize,
            totalUsers,
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
