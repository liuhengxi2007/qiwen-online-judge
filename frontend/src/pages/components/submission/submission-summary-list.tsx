import { Link } from 'react-router-dom'

import { Card, CardContent } from '@/components/ui/card'
import { formatProblemTitleDisplay } from '@/objects/problem/problem-display'
import { problemSlugValue } from '@/objects/problem/problem-parsers'
import { useProblemTitleDisplayMode } from '@/pages/hooks/problem/use-problem-title-display'
import { submissionIdValue, submissionJudgeStateLabel, submissionLanguageLabel } from '@/objects/submission/submission-parsers'
import type { SubmissionSummary } from '@/objects/submission/response/SubmissionSummary'
import {
  formatCodeLength,
  formatOptionalDurationMs,
  formatOptionalMemoryKb,
  formatOptionalScore,
} from '@/objects/submission/submission-display'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import { usernameValue } from '@/objects/user/user-parsers'
import { DateTimeText } from '@/pages/components/date-time-text'
import { UserProfileLink } from '@/pages/components/user/user-profile-link'
import { useI18n } from '@/system/i18n/use-i18n'
import { SubmissionPaginationControls } from '@/pages/components/submission/submission-pagination-controls'

type SubmissionSummaryListProps = {
  submissions: SubmissionSummary[]
  viewer: SessionResponse
  isLoading: boolean
  currentPage: number
  totalPages: number
  pageNumbers: number[]
  onPageChange: (page: number) => void
}

function submissionOverviewStatus(submission: SubmissionSummary): string {
  return submissionJudgeStateLabel(submission.status, submission.verdict)
}

export function SubmissionSummaryList({
  submissions,
  viewer,
  isLoading,
  currentPage,
  totalPages,
  pageNumbers,
  onPageChange,
}: SubmissionSummaryListProps) {
  const { t } = useI18n()
  const problemTitleDisplayMode = useProblemTitleDisplayMode()

  if (!isLoading && submissions.length > 0) {
    return (
      <>
        <div className="mb-6">
          <SubmissionPaginationControls
            currentPage={currentPage}
            totalPages={totalPages}
            pageNumbers={pageNumbers}
            onPageChange={onPageChange}
          />
        </div>
        <div className="space-y-4">
          {submissions.map((submission) => (
            <Card
              key={submissionIdValue(submission.id)}
              className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]"
            >
              <CardContent className="py-3.5">
                <dl className="grid gap-2 text-sm text-slate-600 sm:grid-cols-2 lg:grid-cols-[minmax(0,2fr)_minmax(0,6fr)_minmax(0,6fr)_minmax(0,2fr)_minmax(0,4fr)_minmax(0,6fr)_minmax(0,3fr)_minmax(0,3fr)_minmax(0,3fr)_minmax(0,3fr)]">
                  <div>
                    <dt className="text-slate-500">{t('submission.list.id')}</dt>
                    <dd className="mt-1 font-medium text-slate-900">
                      {submission.canViewDetail ||
                      usernameValue(submission.submitter.username) === usernameValue(viewer.username) ? (
                        <Link
                          className="-mx-2 block min-h-[1.625rem] w-full rounded-lg px-2 py-1 font-medium text-slate-900 transition hover:bg-slate-100 hover:underline"
                          to={`/submissions/${submissionIdValue(submission.id)}`}
                        >
                          {submissionIdValue(submission.id)}
                        </Link>
                      ) : (
                        <span className="block min-h-[1.625rem] w-full px-2 py-1">
                          {submissionIdValue(submission.id)}
                        </span>
                      )}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">{t('submission.list.problem')}</dt>
                    <dd className="mt-1 font-medium text-slate-900">
                      <Link
                        className="-mx-2 block min-h-[1.625rem] w-full rounded-lg px-2 py-1 font-medium text-slate-900 transition hover:bg-slate-100 hover:underline"
                        to={`/problems/${problemSlugValue(submission.problemSlug)}`}
                      >
                        {formatProblemTitleDisplay(submission.problemTitle, submission.problemSlug, problemTitleDisplayMode)}
                      </Link>
                    </dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">{t('submission.list.submitter')}</dt>
                    <dd className="mt-1 font-medium text-slate-900">
                      <UserProfileLink
                        className="block"
                        linkClassName="-mx-2 block min-h-[1.625rem] w-full rounded-lg px-2 py-1 font-medium text-slate-900 transition hover:bg-slate-100 hover:underline"
                        user={submission.submitter}
                      />
                    </dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">{t('submission.list.language')}</dt>
                    <dd className="mt-1 font-medium text-slate-900">
                      <span className="block min-h-[1.625rem] w-full py-1">{submissionLanguageLabel(submission.language)}</span>
                    </dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">{t('common.verdict')}</dt>
                    <dd className="mt-1 font-medium text-slate-900">
                      <span className="block min-h-[1.625rem] w-full py-1">{submissionOverviewStatus(submission)}</span>
                    </dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">{t('common.submittedAt')}</dt>
                    <dd className="mt-1 font-medium text-slate-900">
                      <DateTimeText className="block min-h-[1.625rem] w-full py-1" value={submission.submittedAt} />
                    </dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">{t('submission.list.timeUsed')}</dt>
                    <dd className="mt-1 font-medium text-slate-900">
                      <span className="block min-h-[1.625rem] w-full py-1">{formatOptionalDurationMs(submission.timeUsedMs)}</span>
                    </dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">{t('submission.list.spaceUsed')}</dt>
                    <dd className="mt-1 font-medium text-slate-900">
                      <span className="block min-h-[1.625rem] w-full py-1">{formatOptionalMemoryKb(submission.memoryUsedKb)}</span>
                    </dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">{t('submission.list.score')}</dt>
                    <dd className="mt-1 font-medium text-slate-900">
                      <span className="block min-h-[1.625rem] w-full py-1">{formatOptionalScore(submission.score)}</span>
                    </dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">{t('submission.list.codeLength')}</dt>
                    <dd className="mt-1 font-medium text-slate-900">
                      <span className="block min-h-[1.625rem] w-full py-1">{formatCodeLength(submission.codeLength)}</span>
                    </dd>
                  </div>
                </dl>
              </CardContent>
            </Card>
          ))}
          <SubmissionPaginationControls
            currentPage={currentPage}
            totalPages={totalPages}
            pageNumbers={pageNumbers}
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
