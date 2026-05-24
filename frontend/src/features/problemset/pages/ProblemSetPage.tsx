import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { BookPlus, Layers3 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { problemSetDescriptionValue, problemSetSlugValue, problemSetTitleValue } from '@/features/problemset/lib/problemset-parsers'
import { useProblemSetPageModel } from '@/features/problemset/hooks/use-problemset-page-model'
import { AppSectionBar } from '@/features/auth/components/app-section-bar'
import { resourceAccessBadgeLabel } from '@/shared/domain/resource-lifecycle'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { UserProfileLink } from '@/features/user/components/user-profile-link'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/use-i18n'
import { buildPageNumbers, calculateTotalPages, parsePositivePage } from '@/shared/domain/pagination'
import { usePageSearchParamCorrection } from '@/shared/hooks/use-page-search-param-correction'

const problemSetsPerPage = 10

export function ProblemSetPage() {
  const { t } = useI18n()
  usePageTitle(t('problemSet.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const [searchParams, setSearchParams] = useSearchParams()
  const currentPage = parsePositivePage(searchParams.get('page'))
  const model = useProblemSetPageModel({ page: currentPage, pageSize: problemSetsPerPage })
  const totalPages = calculateTotalPages(model.totalItems, model.pageSize)
  const pageNumbers = buildPageNumbers(currentPage, totalPages)

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

  const canCreate = user.siteManager || user.problemManager

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#fdf8fb_0%,#f4edf7_48%,#ecf3fb_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('problemSet.heading')}</h1>
          </div>

          <AncestorNavigation />
        </div>

        <AppSectionBar />

        <div className="space-y-6">
          {model.errorMessage ? (
            <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
              <AlertDescription className="text-rose-700">{model.errorMessage}</AlertDescription>
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
                  <Button asChild className="rounded-2xl bg-emerald-300 text-emerald-950 hover:bg-emerald-400">
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
                      <Link className="text-lg font-semibold text-slate-950 hover:underline" to={`/problem-sets/${problemSetSlugValue(problemSet.slug)}`}>
                        {problemSetTitleValue(problemSet.title)}
                      </Link>
                      <Badge variant="secondary">{resourceAccessBadgeLabel(problemSet.accessPolicy, t)}</Badge>
                    </div>
                    <p className="mt-2 font-mono text-sm text-slate-500">{problemSetSlugValue(problemSet.slug)}</p>
                    <p className="mt-3 text-sm leading-7 text-slate-600">
                      {problemSetDescriptionValue(problemSet.description) || t('common.noDescription')}
                    </p>
                    <p className="mt-4 text-xs uppercase tracking-[0.18em] text-slate-400">
                      <span>{t('common.createdByLabel')} </span>
                      <UserProfileLink className="inline-flex items-baseline gap-2 normal-case tracking-normal" showUsername user={problemSet.creator} />
                    </p>
                  </div>
                ))
              )}
              {!model.isLoading && model.problemSets.length > 0 && totalPages > 1 ? (
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
        </div>
      </section>
    </main>
  )
}
