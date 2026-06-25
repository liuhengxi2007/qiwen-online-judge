import { Link } from 'react-router-dom'

import { cn } from '@/components/ui/class-names'
import { Card, CardContent } from '@/components/ui/card'
import { formatProblemTitleDisplay } from '@/pages/objects/ProblemTitleDisplay'
import { useProblemTitleDisplayMode } from '@/pages/hooks/useProblemTitleDisplay'
import { submissionIdValue } from '@/objects/submission/SubmissionId'
import type { SubmissionSummary } from '@/objects/submission/response/SubmissionSummary'
import {
  formatCodeLength,
  formatOptionalDurationMs,
  formatOptionalMemoryKb,
  submissionLanguageLabel,
  submissionProblemPath,
  submissionResultLabel,
  submissionResultMotionClassName,
  submissionResultTextStyle,
} from '@/pages/objects/SubmissionDisplay'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import { usernameValue } from '@/objects/user/Username'
import { DateTimeText } from '@/pages/components/DateTimeText'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { PaginationControls } from '@/pages/components/PaginationControls'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 提交摘要列表属性，包含当前页数据、查看者、分页状态和切页回调。
 */
type SubmissionSummaryListProps = {
  submissions: SubmissionSummary[]
  viewer: SessionResponse
  isLoading: boolean
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
}

const submissionListGridClassName =
  'grid grid-cols-[3.5rem_minmax(10.75rem,2fr)_minmax(9rem,1.5fr)_4.5rem_minmax(8rem,1.2fr)_minmax(9.75rem,1.5fr)_5.25rem_6.25rem_5.25rem] items-center gap-2.5'
const submissionHeaderCellClassName = 'min-w-0 px-2'
const submissionValueCellClassName = 'min-w-0 px-2 text-sm font-medium text-slate-900'
const submissionValueTextClassName = 'block py-1'
const submissionValueLinkClassName =
  '-mx-2 block truncate rounded-lg px-2 py-1 font-medium text-slate-900 transition hover:bg-slate-100 hover:underline'

/**
 * 提交摘要列表组件，渲染可横向滚动的提交表格、分页和空/加载状态。
 */
export function SubmissionSummaryList({
  submissions,
  viewer,
  isLoading,
  currentPage,
  totalPages,
  onPageChange,
}: SubmissionSummaryListProps) {
  // 保留扁平 props：摘要表格、查看者和分页状态是列表组件的固定边界，调用端具名字段更清楚。
  const { t } = useI18n()
  const problemTitleDisplayMode = useProblemTitleDisplayMode()

  if (!isLoading && submissions.length > 0) {
    return (
      <>
        <div className="mb-6">
          <PaginationControls
            currentPage={currentPage}
            totalPages={totalPages}
            previousLabel={t('submission.pagination.previous')}
            nextLabel={t('submission.pagination.next')}
            onPageChange={onPageChange}
          />
        </div>
        <div className="space-y-3">
          <div className="overflow-x-auto pb-2">
            <div className="min-w-[71rem] space-y-2">
              <div
                className={cn(
                  submissionListGridClassName,
                  'px-4 py-1.5 text-xs font-semibold uppercase tracking-[0.14em] text-slate-500',
                )}
              >
                <div className={submissionHeaderCellClassName}>{t('submission.list.id')}</div>
                <div className={submissionHeaderCellClassName}>{t('submission.list.problem')}</div>
                <div className={submissionHeaderCellClassName}>{t('submission.list.submitter')}</div>
                <div className={submissionHeaderCellClassName}>{t('submission.list.language')}</div>
                <div className={submissionHeaderCellClassName}>{t('submission.list.result')}</div>
                <div className={submissionHeaderCellClassName}>{t('common.submittedAt')}</div>
                <div className={submissionHeaderCellClassName}>{t('submission.list.timeUsed')}</div>
                <div className={submissionHeaderCellClassName}>{t('submission.list.spaceUsed')}</div>
                <div className={submissionHeaderCellClassName}>{t('submission.list.codeLength')}</div>
              </div>

              {submissions.map((submission) => {
                const canOpenSubmission =
                  submission.canViewDetail ||
                  usernameValue(submission.submitter.username) === usernameValue(viewer.username)

                return (
                  <Card
                    key={submissionIdValue(submission.id)}
                    className="border-slate-200 bg-white py-0 shadow-[0_12px_32px_rgba(15,23,42,0.06)]"
                  >
                    <CardContent className="px-4 py-2">
                      <div className={submissionListGridClassName}>
                        <div className={submissionValueCellClassName}>
                          {canOpenSubmission ? (
                            <Link className={submissionValueLinkClassName} to={`/submissions/${submissionIdValue(submission.id)}`}>
                              {submissionIdValue(submission.id)}
                            </Link>
                          ) : (
                            <span className={submissionValueTextClassName}>{submissionIdValue(submission.id)}</span>
                          )}
                        </div>

                        <div className={submissionValueCellClassName}>
                          <Link
                            className={submissionValueLinkClassName}
                            to={submissionProblemPath(submission.source, submission.problemSlug)}
                          >
                            {formatProblemTitleDisplay(submission.problemTitle, submission.problemSlug, problemTitleDisplayMode)}
                          </Link>
                        </div>

                        <div className={submissionValueCellClassName}>
                          <UserProfileLink
                            className="block min-w-0"
                            linkClassName={submissionValueLinkClassName}
                            user={submission.submitter}
                          />
                        </div>

                        <div className={submissionValueCellClassName}>
                          <span className={submissionValueTextClassName}>{submissionLanguageLabel(submission.language)}</span>
                        </div>

                        <div className={submissionValueCellClassName}>
                          <span
                            className={cn(
                              submissionValueTextClassName,
                              'leading-tight',
                              submissionResultMotionClassName(submission.status, submission.verdict),
                            )}
                            style={submissionResultTextStyle(
                              submission.resultDisplayMode,
                              submission.status,
                              submission.verdict,
                              submission.score,
                            )}
                          >
                            {submissionResultLabel(
                              submission.resultDisplayMode,
                              submission.status,
                              submission.verdict,
                              submission.score,
                            )}
                          </span>
                        </div>

                        <div className={submissionValueCellClassName}>
                          <DateTimeText className={cn(submissionValueTextClassName, 'whitespace-nowrap')} value={submission.submittedAt} />
                        </div>

                        <div className={submissionValueCellClassName}>
                          <span className={cn(submissionValueTextClassName, 'whitespace-nowrap')}>
                            {formatOptionalDurationMs(submission.timeUsedMs)}
                          </span>
                        </div>

                        <div className={submissionValueCellClassName}>
                          <span className={cn(submissionValueTextClassName, 'whitespace-nowrap')}>
                            {formatOptionalMemoryKb(submission.memoryUsedKb)}
                          </span>
                        </div>

                        <div className={submissionValueCellClassName}>
                          <span className={cn(submissionValueTextClassName, 'whitespace-nowrap')}>
                            {formatCodeLength(submission.codeLength)}
                          </span>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                )
              })}
            </div>
          </div>

          <PaginationControls
            currentPage={currentPage}
            totalPages={totalPages}
            previousLabel={t('submission.pagination.previous')}
            nextLabel={t('submission.pagination.next')}
            onPageChange={onPageChange}
          />
        </div>
      </>
    )
  }

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardContent className="py-10 text-sm text-slate-500">
        {isLoading ? t('submission.list.loading') : t('submission.list.empty')}
      </CardContent>
    </Card>
  )
}
