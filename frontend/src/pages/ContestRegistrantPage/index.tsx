import { Navigate, useParams, useSearchParams } from 'react-router-dom'
import { UsersRound } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { parseContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { PageShell } from '@/pages/components/PageShell'
import { PaginationControls } from '@/pages/components/PaginationControls'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { usePageSearchParamCorrection } from '@/pages/hooks/usePageSearchParamCorrection'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { buildPageNumbers, calculateTotalPages, parsePositivePage } from '@/pages/objects/Pagination'
import { useI18n } from '@/system/i18n/use-i18n'
import { useContestRegistrantPageModel } from './hooks/useContestRegistrantPageModel'

const registrantsPerPage = 10

/**
 * 比赛报名用户页入口，校验比赛 slug 和分页参数后渲染报名列表。
 */
export function ContestRegistrantPage() {
  const { t } = useI18n()
  usePageTitle(t('contest.registrants.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const { slug } = useParams<{ slug: string }>()
  const [searchParams, setSearchParams] = useSearchParams()
  const currentPage = parsePositivePage(searchParams.get('page'))
  const slugResult = parseContestSlug(slug ?? '')

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  if (!slugResult.ok) {
    return <Navigate replace to="/contests" />
  }

  return (
    <ContestRegistrantPageContent
      currentPage={currentPage}
      contestSlug={slugResult.value}
      searchParams={searchParams}
      setSearchParams={setSearchParams}
    />
  )
}

/**
 * 比赛报名用户页主体，负责会话守卫、报名列表查询、分页修正和表格展示。
 */
function ContestRegistrantPageContent({
  contestSlug,
  currentPage,
  searchParams,
  setSearchParams,
}: {
  contestSlug: ContestSlug
  currentPage: number
  searchParams: URLSearchParams
  setSearchParams: ReturnType<typeof useSearchParams>[1]
}) {
  const { t } = useI18n()
  const model = useContestRegistrantPageModel(contestSlug, { page: currentPage, pageSize: registrantsPerPage })
  const totalPages = calculateTotalPages(model.totalItems, model.pageSize)
  const pageNumbers = buildPageNumbers(currentPage, totalPages)

  usePageSearchParamCorrection({
    currentPage,
    totalPages,
    isLoading: model.isLoading,
    setSearchParams,
  })

  const onPageChange = (page: number) => {
    const nextSearchParams = new URLSearchParams(searchParams)
    nextSearchParams.set('page', String(page))
    setSearchParams(nextSearchParams)
  }

  return (
    <PageShell title={t('contest.registrants.heading')} mainClassName="bg-[linear-gradient(180deg,#f0fdfa_0%,#ecfeff_48%,#f8fafc_100%)]">
      <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
              <UsersRound className="size-5" />
            </div>
            <CardTitle className="text-xl text-slate-950">{t('contest.registrants.cardTitle')}</CardTitle>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {model.errorMessage ? (
            <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
              <AlertDescription className="text-rose-700">{model.errorMessage}</AlertDescription>
            </Alert>
          ) : null}
          {model.isLoading ? (
            <p className="text-sm text-slate-500">{t('contest.registrants.loading')}</p>
          ) : model.registrants.length === 0 ? (
            <p className="text-sm text-slate-500">{t('contest.registrants.empty')}</p>
          ) : (
            model.registrants.map((registrant) => (
              <div key={registrant.user.username} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <UserProfileLink className="inline-flex items-center gap-2" showUsername user={registrant.user} />
              </div>
            ))
          )}
          {!model.isLoading && model.registrants.length > 0 && totalPages > 1 ? (
            <PaginationControls
              currentPage={currentPage}
              pageNumbers={pageNumbers}
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
