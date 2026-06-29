import { Link } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { formatProblemTitleDisplay } from '@/pages/objects/ProblemTitleDisplay'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { useProblemTitleDisplayMode } from '@/pages/hooks/useProblemTitleDisplay'
import type { UserAcceptedProblem } from '@/objects/user/UserAcceptedProblem'
import { formatDateTime, formatUtcOffsetTitle } from '@/system/format/date-time'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 已通过题目面板属性，包含完整数量、当前页切片和客户端分页控制回调。
 */
type AcceptedProblemsPanelProps = {
  acceptedProblemCount: number
  acceptedProblemsExpanded: boolean
  acceptedProblemsLoadError: string
  acceptedProblemsPageItems: UserAcceptedProblem[]
  acceptedProblemsTotalPages: number
  hasProfile: boolean
  isLoadingAcceptedProblems: boolean
  isLoadingProfile: boolean
  normalizedAcceptedProblemsPage: number
  onNextPage: () => void
  onPreviousPage: () => void
  onToggleExpanded: () => void
}

/**
 * 已通过题目面板，展示通过数量，并在展开后按客户端分页列出题目和通过时间。
 */
export function AcceptedProblemsPanel({
  acceptedProblemCount,
  acceptedProblemsExpanded,
  acceptedProblemsLoadError,
  acceptedProblemsPageItems,
  acceptedProblemsTotalPages,
  hasProfile,
  isLoadingAcceptedProblems,
  isLoadingProfile,
  normalizedAcceptedProblemsPage,
  onNextPage,
  onPreviousPage,
  onToggleExpanded,
}: AcceptedProblemsPanelProps) {
  // 保留扁平 props：该面板同时展示总数、展开状态和当前页数据，调用端具名传入比多层配置对象更易追踪。
  const { t } = useI18n()
  const problemTitleDisplayMode = useProblemTitleDisplayMode()

  return (
    <div className="rounded-3xl bg-emerald-50 p-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-sm font-medium text-emerald-800">{t('userProfile.acceptedProblems')}</p>
          <p className="mt-2 text-3xl font-semibold text-emerald-700">
            {hasProfile ? String(acceptedProblemCount) : isLoadingProfile ? t('common.loading') : '--'}
          </p>
        </div>
        <Button
          disabled={!hasProfile || acceptedProblemCount === 0}
          type="button"
          variant="outline"
          className="rounded-2xl border-emerald-300 bg-white text-emerald-950"
          onClick={onToggleExpanded}
        >
          {acceptedProblemsExpanded ? t('userProfile.hideAcceptedProblems') : t('userProfile.showAcceptedProblems')}
        </Button>
      </div>

      {acceptedProblemsExpanded ? (
        acceptedProblemsLoadError ? (
          <p className="mt-4 text-sm text-rose-700">{acceptedProblemsLoadError}</p>
        ) : isLoadingAcceptedProblems ? (
          <p className="mt-4 text-sm text-emerald-700">{t('common.loading')}</p>
        ) : acceptedProblemCount > 0 ? (
          <div className="mt-4 space-y-2">
            {acceptedProblemsPageItems.length > 0 ? (
              <>
                {acceptedProblemsPageItems.map((problem) => (
                  <div
                    key={problemSlugValue(problem.slug)}
                    className="rounded-2xl border border-emerald-100 bg-white px-4 py-3"
                  >
                    <Link
                      className="font-medium text-slate-900 hover:underline"
                      to={`/problems/${problemSlugValue(problem.slug)}`}
                    >
                      {formatProblemTitleDisplay(problem.title, problem.slug, problemTitleDisplayMode)}
                    </Link>
                    <p className="mt-1 text-sm text-emerald-700">
                      <span title={formatUtcOffsetTitle(problem.acceptedAt)}>
                        {t('userProfile.acceptedAt', { acceptedAt: formatDateTime(problem.acceptedAt) })}
                      </span>
                    </p>
                  </div>
                ))}
                <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
                  <p className="text-sm text-emerald-700">
                    {t('userProfile.acceptedProblemsPageStatus', {
                      page: String(normalizedAcceptedProblemsPage),
                      totalPages: String(acceptedProblemsTotalPages),
                    })}
                  </p>
                  <div className="flex gap-2">
                    <Button
                      disabled={normalizedAcceptedProblemsPage <= 1 || isLoadingAcceptedProblems}
                      type="button"
                      variant="outline"
                      className="rounded-2xl border-emerald-300 bg-white text-emerald-950"
                      onClick={onPreviousPage}
                    >
                      {t('submission.pagination.previous')}
                    </Button>
                    <Button
                      disabled={normalizedAcceptedProblemsPage >= acceptedProblemsTotalPages || isLoadingAcceptedProblems}
                      type="button"
                      variant="outline"
                      className="rounded-2xl border-emerald-300 bg-white text-emerald-950"
                      onClick={onNextPage}
                    >
                      {t('submission.pagination.next')}
                    </Button>
                  </div>
                </div>
              </>
            ) : (
              <p className="text-sm text-emerald-700">{t('common.emptyData')}</p>
            )}
          </div>
        ) : (
          <p className="mt-4 text-sm text-emerald-700">{t('userProfile.acceptedProblemsEmpty')}</p>
        )
      ) : null}
    </div>
  )
}
