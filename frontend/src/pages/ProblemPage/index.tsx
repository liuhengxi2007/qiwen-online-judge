import { useEffect, useState } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { useSessionGuard } from '@/pages/hooks/use-session-guard'
import { parseProblemSearchQuery } from '@/objects/problem/problem-parsers'
import { shouldShowProblemSlugSupplement } from '@/objects/problem/problem-display'
import { useProblemPageModel } from './hooks/use-problem-page-model'
import { useProblemTitleDisplayMode } from '@/pages/hooks/use-problem-title-display'
import { AppSectionBar } from '@/pages/components/app-section-bar'
import {
  buildPageNumbers,
  calculateTotalPages,
  parsePositivePage,
} from '@/objects/shared/pagination'
import { AncestorNavigation } from '@/pages/components/ancestor-navigation'
import { usePageTitle } from '@/pages/hooks/use-page-title'
import { useI18n } from '@/system/i18n/use-i18n'
import { usePageSearchParamCorrection } from '@/pages/hooks/use-page-search-param-correction'

import { ProblemListCard } from './components/ProblemListCard'

const problemsPerPage = 10

export function ProblemPage() {
  const { t } = useI18n()
  usePageTitle(t('problem.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return <ProblemPageContent canCreate={user.siteManager || user.problemManager} />
}

function ProblemPageContent({ canCreate }: { canCreate: boolean }) {
  const { t } = useI18n()
  const problemTitleDisplayMode = useProblemTitleDisplayMode()
  const [searchParams, setSearchParams] = useSearchParams()
  const activeQuery = searchParams.get('q')?.trim() ?? ''
  const [queryInput, setQueryInput] = useState(activeQuery)
  const currentPage = parsePositivePage(searchParams.get('page'))
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

  usePageSearchParamCorrection({
    currentPage,
    totalPages,
    isLoading: model.isLoading,
    setSearchParams,
  })

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

  function clearQuery() {
    setQueryInput('')
    const nextSearchParams = new URLSearchParams(searchParams)
    nextSearchParams.delete('q')
    nextSearchParams.delete('page')
    setSearchParams(nextSearchParams)
  }

  function setPage(page: number) {
    const nextSearchParams = new URLSearchParams(searchParams)
    nextSearchParams.set('page', String(page))
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

          <ProblemListCard
            canCreate={canCreate}
            currentPage={currentPage}
            isLoading={model.isLoading}
            onApplyQuery={applyQuery}
            onClearQuery={clearQuery}
            onPageChange={setPage}
            pageNumbers={pageNumbers}
            problems={model.problems}
            queryInput={queryInput}
            setQueryInput={setQueryInput}
            showSlugSupplement={shouldShowProblemSlugSupplement(problemTitleDisplayMode)}
            totalPages={totalPages}
          />
        </div>
      </section>
    </main>
  )
}
