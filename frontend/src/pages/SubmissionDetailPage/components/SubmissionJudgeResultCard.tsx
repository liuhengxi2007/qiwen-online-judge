import type { CSSProperties } from 'react'
import { Link } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { submissionIdValue } from '@/objects/submission/SubmissionId'
import {
  formatOptionalDurationMs,
  formatOptionalMemoryKb,
  formatOptionalScore,
  formatTestcaseDetail,
  submissionVerdictTextStyle,
  submissionVerdictLabel,
} from '@/pages/objects/SubmissionDisplay'
import type { JudgeResult } from '@/objects/submission/JudgeResult'
import type { JudgeResultSummary } from '@/objects/submission/JudgeResultSummary'
import { scoreTextStyleForRatio } from '@/pages/objects/ScoreDisplay'
import { useI18n } from '@/system/i18n/use-i18n'

type SubmissionJudgeResultCardProps = {
  judgeResult: JudgeResult
  submissionId: SubmissionId
}

export function SubmissionJudgeResultCard({ judgeResult, submissionId }: SubmissionJudgeResultCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <CardTitle className="text-xl text-slate-950">{t('submission.detail.judgeResult')}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid gap-3 text-sm lg:grid-cols-2">
          <ResultSummaryGroup
            label={t('submission.detail.baseResult')}
            summary={judgeResult.baseResult}
            verdictLabel={t('common.verdict')}
            scoreLabel={t('submission.list.score')}
            timeLabel={t('submission.list.timeUsed')}
            memoryLabel={t('submission.list.spaceUsed')}
            reasonLabel={t('submission.detail.reason')}
          />
          <ResultSummaryGroup
            label={t('submission.detail.worstResult')}
            summary={judgeResult.worstResult}
            verdictLabel={t('common.verdict')}
            scoreLabel={t('submission.list.score')}
            timeLabel={t('submission.list.timeUsed')}
            memoryLabel={t('submission.list.spaceUsed')}
            reasonLabel={t('submission.detail.reason')}
          />
        </div>

        {judgeResult.subtasks.map((subtask) => {
          const canHackSubtask = subtask.worstResult.score > 0

          return (
            <div key={subtask.index} className="rounded-lg border border-slate-200">
              <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-200 px-4 py-3">
                <div className="min-w-[min(100%,28rem)] flex-1">
                  <p className="font-medium text-slate-950">{resultNodeTitle('subtask', subtask.index, subtask.label)}</p>
                  <div className="mt-3 grid gap-3 text-sm lg:grid-cols-2">
                    <ResultSummaryGroup
                      label={t('submission.detail.baseResult')}
                      summary={subtask.baseResult}
                      verdictLabel={t('common.verdict')}
                      scoreLabel={t('submission.list.score')}
                      timeLabel={t('submission.list.timeUsed')}
                      memoryLabel={t('submission.list.spaceUsed')}
                      reasonLabel={t('submission.detail.reason')}
                    />
                    <ResultSummaryGroup
                      label={t('submission.detail.worstResult')}
                      summary={subtask.worstResult}
                      verdictLabel={t('common.verdict')}
                      scoreLabel={t('submission.list.score')}
                      timeLabel={t('submission.list.timeUsed')}
                      memoryLabel={t('submission.list.spaceUsed')}
                      reasonLabel={t('submission.detail.reason')}
                    />
                  </div>
                </div>
                {canHackSubtask ? (
                  <Button asChild size="sm" variant="outline">
                    <Link to={`/submissions/${submissionIdValue(submissionId)}/hack/${subtask.index}`}>{t('hack.action')}</Link>
                  </Button>
                ) : null}
              </div>
              {subtask.testcases.length > 0 ? (
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[840px] text-left text-sm">
                    <thead className="bg-slate-50 text-slate-500">
                      <tr>
                        <th className="px-4 py-2 font-medium">{t('submission.detail.testcases')}</th>
                        <th className="px-4 py-2 font-medium">{t('common.verdict')}</th>
                        <th className="px-4 py-2 font-medium">{t('submission.list.score')}</th>
                        <th className="px-4 py-2 font-medium">{t('submission.list.timeUsed')}</th>
                        <th className="px-4 py-2 font-medium">{t('submission.list.spaceUsed')}</th>
                        <th className="px-4 py-2 font-medium">{t('submission.detail.testcaseDetail')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {subtask.testcases.map((testcase) => (
                        <tr key={testcase.index} className="border-t border-slate-100">
                          <td className="px-4 py-2 font-medium text-slate-900">{resultNodeTitle('testcase', testcase.index, testcase.label)}</td>
                          <td className="px-4 py-2 text-slate-700" style={submissionVerdictTextStyle(testcase.verdict)}>
                            {submissionVerdictLabel(testcase.verdict)}
                          </td>
                          <td className="px-4 py-2 text-slate-700" style={scoreTextStyleForRatio(testcase.score)}>
                            {formatOptionalScore(testcase.score)}
                          </td>
                          <td className="px-4 py-2 text-slate-700">{formatOptionalDurationMs(testcase.timeUsedMs)}</td>
                          <td className="px-4 py-2 text-slate-700">{formatOptionalMemoryKb(testcase.memoryUsedKb)}</td>
                          <td className="max-w-[320px] whitespace-pre-wrap px-4 py-2 text-slate-700">
                            {formatTestcaseDetail(testcase.reason, testcase.message)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="px-4 py-3 text-sm text-slate-500">{t('submission.detail.noTestcases')}</p>
              )}
            </div>
          )
        })}
      </CardContent>
    </Card>
  )
}

function ResultSummaryGroup({
  label,
  summary,
  verdictLabel,
  scoreLabel,
  timeLabel,
  memoryLabel,
  reasonLabel,
}: {
  label: string
  summary: JudgeResultSummary
  verdictLabel: string
  scoreLabel: string
  timeLabel: string
  memoryLabel: string
  reasonLabel: string
}) {
  return (
    <div className="rounded-md border border-slate-200 px-3 py-2">
      <p className="text-xs uppercase tracking-normal text-slate-500">{label}</p>
      <div className="mt-2 grid gap-2 sm:grid-cols-4">
        <MetricValue
          label={verdictLabel}
          value={submissionVerdictLabel(summary.verdict)}
          valueStyle={submissionVerdictTextStyle(summary.verdict)}
        />
        <MetricValue label={scoreLabel} value={formatOptionalScore(summary.score)} valueStyle={scoreTextStyleForRatio(summary.score)} />
        <MetricValue label={timeLabel} value={formatOptionalDurationMs(summary.timeUsedMs)} />
        <MetricValue label={memoryLabel} value={formatOptionalMemoryKb(summary.memoryUsedKb)} />
      </div>
      <JudgeReasonLine label={reasonLabel} reason={summary.reason} />
    </div>
  )
}

function MetricValue({
  label,
  value,
  valueStyle,
}: {
  label: string
  value: string
  valueStyle?: CSSProperties
}) {
  return (
    <div>
      <p className="text-[0.7rem] uppercase tracking-normal text-slate-400">{label}</p>
      <p className="mt-0.5 font-medium text-slate-950" style={valueStyle}>{value}</p>
    </div>
  )
}

function resultNodeTitle(kind: 'subtask' | 'testcase', index: number, label: string | null): string {
  return label ? `${kind} ${index} (${label})` : `${kind} ${index}`
}

function JudgeReasonLine({ label, reason }: { label: string; reason: string | null }) {
  if (!reason) {
    return null
  }

  return (
    <p className="mt-1 text-xs text-slate-500">
      {label}: <span className="font-mono text-slate-700">{reason}</span>
    </p>
  )
}
