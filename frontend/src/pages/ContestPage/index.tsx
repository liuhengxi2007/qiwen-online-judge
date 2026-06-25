import { Navigate, useSearchParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { PageShell } from '@/pages/components/PageShell'
import { usePageSearchParamCorrection } from '@/pages/hooks/usePageSearchParamCorrection'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { calculateTotalPages, parsePositivePage } from '@/pages/objects/Pagination'
import { useI18n } from '@/system/i18n/use-i18n'
import { ContestListCard } from './components/ContestListCard'
import { useContestPageModel } from './hooks/useContestPageModel'
import { useNow } from './hooks/useNow'

const contestsPerPage = 10

/**
 * 比赛列表页，负责会话守卫、分页查询、创建入口和列表展示。
 */
export function ContestPage() {
  const { t } = useI18n()
  usePageTitle(t('contest.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const [searchParams, setSearchParams] = useSearchParams()
  const currentPage = parsePositivePage(searchParams.get('page'))
  const model = useContestPageModel({ page: currentPage, pageSize: contestsPerPage })
  const now = useNow()
  const totalPages = calculateTotalPages(model.totalItems, model.pageSize)

  usePageSearchParamCorrection({
    currentPage,
    totalPages,
    isLoading: model.isLoading,
    setSearchParams,
  })

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const canCreate = user.contestManager
  const onPageChange = (page: number) => {
    const nextSearchParams = new URLSearchParams(searchParams)
    nextSearchParams.set('page', String(page))
    nextSearchParams.delete('created')
    setSearchParams(nextSearchParams)
  }

  return (
    <PageShell title={t('contest.heading')} mainClassName="bg-[linear-gradient(180deg,#f0fdfa_0%,#ecfeff_48%,#f8fafc_100%)]">
      <div className="space-y-6">
        {model.errorMessage ? (
          <Alert variant="destructive">
            <AlertDescription>{model.errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {searchParams.get('created') ? (
          <Alert variant="success">
            <AlertDescription>{t('contest.list.created')}</AlertDescription>
          </Alert>
        ) : null}

        {model.registrationMessage ? (
          <Alert variant="success">
            <AlertDescription>{model.registrationMessage}</AlertDescription>
          </Alert>
        ) : null}

        <ContestListCard
          activeRegistrationSlug={model.activeRegistrationSlug}
          canCreate={canCreate}
          contests={model.contests}
          currentPage={currentPage}
          isLoading={model.isLoading}
          now={now}
          onPageChange={onPageChange}
          onToggleRegistration={(contest) => void model.toggleRegistration(contest)}
          totalPages={totalPages}
        />
      </div>
    </PageShell>
  )
}
