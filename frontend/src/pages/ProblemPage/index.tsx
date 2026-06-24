import { useEffect, useState } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { parseProblemSearchQuery } from '@/objects/problem/request/ProblemSearchQuery'
import { shouldShowProblemSlugSupplement } from '@/pages/objects/ProblemTitleDisplay'
import { useProblemPageModel } from './hooks/useProblemPageModel'
import { useProblemTitleDisplayMode } from '@/pages/hooks/useProblemTitleDisplay'
import {
  buildPageNumbers,
  calculateTotalPages,
  parsePositivePage,
} from '@/pages/objects/Pagination'
import { PageShell } from '@/pages/components/PageShell'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'
import { usePageSearchParamCorrection } from '@/pages/hooks/usePageSearchParamCorrection'

import { ProblemListCard } from './components/ProblemListCard'

const problemsPerPage = 10

/**
 * 题目列表页入口，完成会话守卫并根据权限展示创建入口。
 */
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

  return <ProblemPageContent canCreate={user.problemManager} />
}

/**
 * 题目列表页主体，解析分页、加载题目列表并渲染分页修正。
 */
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
    <PageShell title={t('problem.list.heading')} mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#edf5f1_100%)]">
      <div className="space-y-6">
        {model.errorMessage ? (
          <Alert variant="destructive">
            <AlertDescription>{model.errorMessage}</AlertDescription>
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
    </PageShell>
  )
}
