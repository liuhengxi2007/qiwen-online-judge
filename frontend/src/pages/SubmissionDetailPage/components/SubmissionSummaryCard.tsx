import { Link } from 'react-router-dom'
import { Files, RotateCcw, Trash2 } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { ConfirmActionDialog } from '@/pages/components/ConfirmActionDialog'
import { DateTimeText } from '@/pages/components/DateTimeText'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { formatProblemTitleDisplay } from '@/pages/objects/ProblemTitleDisplay'
import type { ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import { isTerminalSubmissionStatus } from '@/objects/submission/SubmissionStatus'
import { submissionIdValue } from '@/objects/submission/SubmissionId'
import {
  formatCodeLength,
  formatOptionalDurationMs,
  formatOptionalMemoryKb,
  submissionLanguageLabel,
  submissionProblemPath,
  submissionResultLabel,
} from '@/pages/objects/SubmissionDisplay'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'
import { useI18n } from '@/system/i18n/use-i18n'

type SubmissionSummaryCardProps = {
  deleteCurrentSubmission: () => Promise<void>
  isDeleting: boolean
  isRejudging: boolean
  problemTitleDisplayMode: ProblemTitleDisplayMode
  rejudge: () => Promise<void>
  submission: SubmissionDetail
}

export function SubmissionSummaryCard({
  deleteCurrentSubmission,
  isDeleting,
  isRejudging,
  problemTitleDisplayMode,
  rejudge,
  submission,
}: SubmissionSummaryCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-indigo-100 text-indigo-700">
              <Files className="size-5" />
            </div>
            <div>
              <CardTitle className="text-2xl text-slate-950">
                Submission {submissionIdValue(submission.id)}
              </CardTitle>
              <CardDescription className="mt-2 text-sm text-slate-500">{t('submission.detail.heading')}</CardDescription>
            </div>
          </div>

          {submission.canManage ? (
            <div className="flex flex-wrap gap-3">
              <Button
                type="button"
                variant="outline"
                className="rounded-2xl border-slate-300 bg-white"
                disabled={isRejudging || isDeleting || !isTerminalSubmissionStatus(submission.status)}
                onClick={() => {
                  void rejudge()
                }}
              >
                <RotateCcw className="mr-2 size-4" />
                {isRejudging ? t('submission.detail.rejudgingAction') : t('submission.detail.rejudgeAction')}
              </Button>

              <ConfirmActionDialog
                title={t('submission.detail.deleteConfirmTitle')}
                description={t('submission.detail.deleteConfirmDescription')}
                confirmLabel={isDeleting ? t('submission.detail.deletingAction') : t('submission.detail.deleteAction')}
                destructive
                onConfirm={() => {
                  void deleteCurrentSubmission()
                }}
                trigger={
                  <Button
                    type="button"
                    variant="outline"
                    className="rounded-2xl border-rose-300 bg-white text-rose-700 hover:bg-rose-100 hover:text-rose-800"
                    disabled={isDeleting || isRejudging}
                  >
                    <Trash2 className="mr-2 size-4" />
                    {isDeleting ? t('submission.detail.deletingAction') : t('submission.detail.deleteAction')}
                  </Button>
                }
              />
            </div>
          ) : null}
        </div>
      </CardHeader>
      <CardContent className="grid gap-4 text-sm text-slate-600 sm:grid-cols-2 lg:grid-cols-8">
        <div>
          <p className="text-slate-500">{t('submission.list.problem')}</p>
          <p className="mt-1">
            <Link
              className="font-medium text-slate-900 hover:underline"
              to={submissionProblemPath(submission.source, submission.problemSlug)}
            >
              {formatProblemTitleDisplay(
                submission.problemTitle,
                submission.problemSlug,
                problemTitleDisplayMode,
              )}
            </Link>
          </p>
        </div>
        <div>
          <p className="text-slate-500">{t('submission.detail.submitter')}</p>
          <div className="mt-1">
            <UserProfileLink user={submission.submitter} />
          </div>
        </div>
        <div>
          <p className="text-slate-500">{t('common.languageLabel')}</p>
          <p className="mt-1 font-medium text-slate-900">
            {submissionLanguageLabel(submission.language)}
          </p>
        </div>
        <div>
          <p className="text-slate-500">{t('submission.list.result')}</p>
          <p className="mt-1 font-medium text-slate-900">
            {submissionResultLabel(
              submission.resultDisplayMode,
              submission.status,
              submission.verdict,
              submission.score,
            )}
          </p>
        </div>
        <div>
          <p className="text-slate-500">{t('common.submittedAt')}</p>
          <DateTimeText className="mt-1 font-medium text-slate-900" value={submission.submittedAt} />
        </div>
        <div>
          <p className="text-slate-500">{t('submission.list.timeUsed')}</p>
          <p className="mt-1 font-medium text-slate-900">
            {formatOptionalDurationMs(submission.timeUsedMs)}
          </p>
        </div>
        <div>
          <p className="text-slate-500">{t('submission.list.spaceUsed')}</p>
          <p className="mt-1 font-medium text-slate-900">
            {formatOptionalMemoryKb(submission.memoryUsedKb)}
          </p>
        </div>
        <div>
          <p className="text-slate-500">{t('submission.list.codeLength')}</p>
          <p className="mt-1 font-medium text-slate-900">{formatCodeLength(submission.codeLength)}</p>
        </div>
      </CardContent>
    </Card>
  )
}
