import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { BookPlus, Layers3 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { problemSetDescriptionValue } from '@/objects/problemset/ProblemSetDescription'
import { problemSetSlugValue } from '@/objects/problemset/ProblemSetSlug'
import { problemSetTitleValue } from '@/objects/problemset/ProblemSetTitle'
import { useProblemSetPageModel } from './hooks/useProblemSetPageModel'
import { PageShell } from '@/pages/components/PageShell'
import { PaginationControls } from '@/pages/components/PaginationControls'
import { resourceAccessBadgeLabel } from '@/pages/objects/ResourceAccessDisplay'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'
import { calculateTotalPages, parsePositivePage } from '@/pages/objects/Pagination'
import { usePageSearchParamCorrection } from '@/pages/hooks/usePageSearchParamCorrection'

const problemSetsPerPage = 10

/**
 * 题单列表页，负责会话守卫、分页查询、创建入口和题单列表展示。
 */
export function ProblemSetPage() {
  const { t } = useI18n()
  usePageTitle(t('problemSet.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const [searchParams, setSearchParams] = useSearchParams()
  const currentPage = parsePositivePage(searchParams.get('page'))
  const model = useProblemSetPageModel({ page: currentPage, pageSize: problemSetsPerPage })
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

  const canCreate = user.problemManager
  const onPageChange = (page: number) => {
    const nextSearchParams = new URLSearchParams(searchParams)
    nextSearchParams.set('page', String(page))
    setSearchParams(nextSearchParams)
  }

  return (
    <PageShell
      title={t('problemSet.heading')}
      mainClassName="bg-[linear-gradient(180deg,#fdf8fb_0%,#f4edf7_48%,#ecf3fb_100%)]"
    >
      <div className="space-y-6">
        {model.errorMessage ? (
          <Alert variant="destructive">
            <AlertDescription>{model.errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
          <CardHeader>
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-rose-100 text-rose-700">
                  <Layers3 className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">{t('problemSet.list.cardTitle')}</CardTitle>
                  <CardDescription>{t('problemSet.list.cardDescription')}</CardDescription>
                </div>
              </div>
              {canCreate ? (
                <Button asChild variant="create">
                  <Link to="/problem-sets/new">
                    <BookPlus className="size-4" />
                    {t('problemSet.list.create')}
                  </Link>
                </Button>
              ) : null}
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {model.isLoading ? (
              <p className="text-sm text-slate-500">{t('problemSet.list.loading')}</p>
            ) : model.problemSets.length === 0 ? (
              <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center">
                <p className="text-base font-medium text-slate-900">{t('problemSet.list.emptyTitle')}</p>
                <p className="mt-2 text-sm leading-7 text-slate-600">{t('problemSet.list.emptyDescription')}</p>
              </div>
            ) : (
              model.problemSets.map((problemSet) => (
                <div key={problemSet.id} className="rounded-3xl border border-slate-200 bg-slate-50 p-5">
                  <div className="flex flex-wrap items-center gap-3">
                    <Link
                      className="text-lg font-semibold text-slate-950 hover:underline"
                      to={`/problem-sets/${problemSetSlugValue(problemSet.slug)}`}
                    >
                      {problemSetTitleValue(problemSet.title)}
                    </Link>
                    <Badge variant="secondary">{resourceAccessBadgeLabel(problemSet.accessPolicy, t)}</Badge>
                  </div>
                  <p className="mt-2 font-mono text-sm text-slate-500">{problemSetSlugValue(problemSet.slug)}</p>
                  <p className="mt-3 text-sm leading-7 text-slate-600">
                    {problemSetDescriptionValue(problemSet.description) || t('common.noDescription')}
                  </p>
                  <p className="mt-4 text-xs uppercase tracking-[0.18em] text-slate-400">
                    <span>{t('common.authorLabel')} </span>
                    {problemSet.author ? (
                      <UserProfileLink
                        className="inline-flex items-baseline gap-2 normal-case tracking-normal"
                        showUsername
                        user={problemSet.author}
                      />
                    ) : (
                      <span className="normal-case tracking-normal">{t('common.noAuthor')}</span>
                    )}
                  </p>
                </div>
              ))
            )}
            {!model.isLoading && model.problemSets.length > 0 && totalPages > 1 ? (
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
      </div>
    </PageShell>
  )
}
