import { useEffect, useState } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { FilePlus2, LibraryBig } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import {
  parseProblemSearchQuery,
  problemSlugValue,
  useProblemTitleDisplay,
  useProblemTitleDisplayMode,
  shouldShowProblemSlugSupplement,
} from '@/features/problem/domain/problem'
import { useProblemPageModel } from '@/features/problem/hooks/use-problem-page-model'
import { AppSectionBar } from '@/shared/components/app-section-bar'
import { resourceAccessBadgeLabel } from '@/shared/domain/resource-lifecycle'
import {
  buildPageNumbers,
  calculateTotalPages,
  getPageCorrection,
  parsePositivePage,
} from '@/shared/domain/pagination'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { UserProfileLink } from '@/shared/components/user-profile-link'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/i18n'

const problemsPerPage = 10

export function ProblemPage() {
  const { t } = useI18n()
  usePageTitle(t('problem.pageTitle'))
  const problemTitleDisplayMode = useProblemTitleDisplayMode()
  const { session: user, navigationIntent } = useSessionGuard()
  const [searchParams, setSearchParams] = useSearchParams()
  const activeQuery = searchParams.get('q')?.trim() ?? ''
  const [queryInput, setQueryInput] = useState(activeQuery)
  const currentPage = parsePositivePage(searchParams.get('page'))

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const canCreate = user.siteManager || user.problemManager
  const model = useProblemPageModel({
    query: (() => {
      if (!activeQuery) {
        return null
      }
      const parsedQuery = parseProblemSearchQuery(activeQuery)
      return parsedQuery.ok ? parsedQuery.value : null
    })(),
    pageRequest: {
      page: currentPage,
      pageSize: problemsPerPage,
    },
  })
  const totalPages = calculateTotalPages(model.totalItems, model.pageSize)
  const pageNumbers = buildPageNumbers(currentPage, totalPages)

  useEffect(() => {
    setQueryInput(activeQuery)
  }, [activeQuery])

  useEffect(() => {
    if (model.isLoading) {
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
  }, [currentPage, model.isLoading, searchParams, setSearchParams, totalPages])

  function applyQuery() {
    const nextSearchParams = new URLSearchParams(searchParams)
    if (!queryInput.trim()) {
      nextSearchParams.delete('q')
    } else {
      nextSearchParams.set('q', queryInput.trim())
    }
    nextSearchParams.delete('page')
    setSearchParams(nextSearchParams)
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#edf5f1_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('problem.list.heading')}</h1>
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
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                    <LibraryBig className="size-5" />
                  </div>
                  <div>
                    <CardTitle className="text-xl text-slate-950">{t('problem.list.cardTitle')}</CardTitle>
                    <CardDescription>
                      {t('problem.list.cardDescription')}
                    </CardDescription>
                  </div>
                </div>
                {canCreate ? (
                  <Button asChild className="rounded-2xl bg-emerald-300 text-emerald-950 hover:bg-emerald-400">
                    <Link to="/problems/new">
                      <FilePlus2 className="size-4" />
                      {t('problem.list.create')}
                    </Link>
                  </Button>
                ) : null}
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
                <div className="flex-1 space-y-2">
                  <Label htmlFor="problem-search">{t('problem.list.searchLabel')}</Label>
                  <Input
                    id="problem-search"
                    value={queryInput}
                    placeholder={t('problem.list.searchPlaceholder')}
                    onChange={(event) => {
                      setQueryInput(event.target.value)
                    }}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') {
                        event.preventDefault()
                        applyQuery()
                      }
                    }}
                  />
                </div>
                <div className="flex gap-3">
                  <Button type="button" className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800" onClick={applyQuery}>
                    {t('problem.list.searchApply')}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    className="rounded-2xl border-slate-300 bg-white"
                    onClick={() => {
                      setQueryInput('')
                      const nextSearchParams = new URLSearchParams(searchParams)
                      nextSearchParams.delete('q')
                      nextSearchParams.delete('page')
                      setSearchParams(nextSearchParams)
                    }}
                  >
                    {t('problem.list.searchClear')}
                  </Button>
                </div>
              </div>

              {model.isLoading ? (
                <p className="text-sm text-slate-500">{t('problem.list.loading')}</p>
              ) : model.problems.length === 0 ? (
                <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center">
                  <p className="text-base font-medium text-slate-900">{t('problem.list.emptyTitle')}</p>
                  <p className="mt-2 text-sm leading-7 text-slate-600">
                    {t('problem.list.emptyDescription')}
                  </p>
                </div>
              ) : (
                model.problems.map((problem) => (
                  <ProblemListItem
                    key={problem.id}
                    problem={problem}
                    showSlugSupplement={shouldShowProblemSlugSupplement(problemTitleDisplayMode)}
                  />
                ))
              )}
              {!model.isLoading && model.problems.length > 0 && totalPages > 1 ? (
                <div className="flex flex-wrap items-center justify-center gap-2 pt-4">
                  <Button
                    type="button"
                    variant="outline"
                    className="rounded-2xl border-slate-300 bg-white"
                    disabled={currentPage === 1}
                    onClick={() => {
                      const nextSearchParams = new URLSearchParams(searchParams)
                      nextSearchParams.set('page', String(Math.max(1, currentPage - 1)))
                      setSearchParams(nextSearchParams)
                    }}
                  >
                    {t('common.pagination.previous')}
                  </Button>
                  {pageNumbers.map((page) => (
                    <Button
                      key={page}
                      type="button"
                      variant={page === currentPage ? 'default' : 'outline'}
                      className={page === currentPage ? 'rounded-2xl bg-slate-950 text-white' : 'rounded-2xl border-slate-300 bg-white'}
                      onClick={() => {
                        const nextSearchParams = new URLSearchParams(searchParams)
                        nextSearchParams.set('page', String(page))
                        setSearchParams(nextSearchParams)
                      }}
                    >
                      {page}
                    </Button>
                  ))}
                  <Button
                    type="button"
                    variant="outline"
                    className="rounded-2xl border-slate-300 bg-white"
                    disabled={currentPage === totalPages}
                    onClick={() => {
                      const nextSearchParams = new URLSearchParams(searchParams)
                      nextSearchParams.set('page', String(Math.min(totalPages, currentPage + 1)))
                      setSearchParams(nextSearchParams)
                    }}
                  >
                    {t('common.pagination.next')}
                  </Button>
                </div>
              ) : null}
            </CardContent>
          </Card>
        </div>
      </section>
    </main>
  )
}

function ProblemListItem({
  problem,
  showSlugSupplement,
}: {
  problem: (ReturnType<typeof useProblemPageModel>)['problems'][number]
  showSlugSupplement: boolean
}) {
  const { t } = useI18n()
  const titleText = useProblemTitleDisplay(problem.title, problem.slug)

  return (
    <div className="rounded-3xl border border-slate-200 bg-slate-50 p-5">
      <div className="flex flex-wrap items-center gap-3">
        <Link className="text-lg font-semibold text-slate-950 hover:underline" to={`/problems/${problemSlugValue(problem.slug)}`}>
          {titleText}
        </Link>
        <Badge variant="secondary">{resourceAccessBadgeLabel(problem.accessPolicy)}</Badge>
      </div>
      {showSlugSupplement ? <p className="mt-2 font-mono text-sm text-slate-500">{problemSlugValue(problem.slug)}</p> : null}
      <p className="mt-4 text-xs uppercase tracking-[0.18em] text-slate-400">
        <span>{t('problem.createdByLabel')} </span>
        <UserProfileLink className="inline-flex items-baseline gap-2 normal-case tracking-normal" showUsername user={problem.creator} />
      </p>
    </div>
  )
}
