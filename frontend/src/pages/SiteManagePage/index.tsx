import { useEffect, useState } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'

import { SiteManageJudgersCard } from './components/SiteManageJudgersCard'
import { SiteManageUserCard } from './components/SiteManageUserCard'
import { AncestorNavigation } from '@/pages/components/AncestorNavigation'
import { PageShell } from '@/pages/components/PageShell'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSiteManageModel } from './hooks/useSiteManageModel'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { parseUserSearchQuery } from '@/objects/user/request/UserSearchQuery'
import { calculateTotalPages, parsePositivePage } from '@/pages/objects/Pagination'
import { useI18n } from '@/system/i18n/use-i18n'
import { usePageSearchParamCorrection } from '@/pages/hooks/usePageSearchParamCorrection'

/**
 * 站点管理页，要求站点管理员权限并组合用户管理与判题机状态卡片。
 */
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

  usePageSearchParamCorrection({
    currentPage,
    totalPages,
    isLoading: isLoadingUsers,
    setSearchParams,
  })

  if (guardNavigationIntent) {
    return <Navigate replace={guardNavigationIntent.replace} to={guardNavigationIntent.to} />
  }

  if (modelNavigationIntent) {
    return <Navigate replace={modelNavigationIntent.replace} to={modelNavigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const siteManageModel = {
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
  }

  const applyQuery = () => {
    const nextSearchParams = new URLSearchParams(searchParams)
    if (!queryInput.trim()) {
      nextSearchParams.delete('q')
    } else {
      nextSearchParams.set('q', queryInput.trim())
    }
    nextSearchParams.delete('page')
    setSearchParams(nextSearchParams)
  }

  const clearQuery = () => {
    setQueryInput('')
    const nextSearchParams = new URLSearchParams(searchParams)
    nextSearchParams.delete('q')
    nextSearchParams.delete('page')
    setSearchParams(nextSearchParams)
  }

  const setPage = (page: number) => {
    const nextSearchParams = new URLSearchParams(searchParams)
    nextSearchParams.set('page', String(page))
    setSearchParams(nextSearchParams)
  }

  return (
    <PageShell
      title={t('siteManage.heading')}
      mainClassName="bg-[linear-gradient(180deg,#fffaf4_0%,#f4efe5_100%)]"
      siteNameClassName="text-stone-500"
      titleClassName="text-stone-950"
      action={<AncestorNavigation buttonClassName="rounded-full border-stone-300 bg-white" />}
    >
      <SiteManageUserCard
        model={siteManageModel}
        siteManagerSession={Boolean(siteManagerSession)}
        queryInput={queryInput}
        hasActiveQuery={Boolean(activeQuery)}
        onQueryInputChange={setQueryInput}
        onApplyQuery={applyQuery}
        onClearQuery={clearQuery}
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={setPage}
      />
      <SiteManageJudgersCard model={siteManageModel} />
    </PageShell>
  )
}
