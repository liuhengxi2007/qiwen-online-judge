import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  formatOptionalDurationMs,
  formatOptionalMemoryKb,
  formatOptionalScore,
} from '@/pages/objects/SubmissionDisplay'
import { submissionVerdictLabel } from '@/pages/objects/SubmissionDisplay'
import type { JudgeResult } from '@/objects/submission/JudgeResult'
import { useI18n } from '@/system/i18n/use-i18n'

type SubmissionJudgeResultCardProps = {
  judgeResult: JudgeResult
}

export function SubmissionJudgeResultCard({ judgeResult }: SubmissionJudgeResultCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <CardTitle className="text-xl text-slate-950">{t('submission.detail.judgeResult')}</CardTitle>
        <CardDescription>
          {submissionVerdictLabel(judgeResult.verdict)}{' '}
          · {formatOptionalScore(judgeResult.score)}
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid gap-3 text-sm sm:grid-cols-4">
          <ResultMetric label={t('common.verdict')} value={submissionVerdictLabel(judgeResult.verdict)} />
          <ResultMetric label={t('submission.list.score')} value={formatOptionalScore(judgeResult.score)} />
          <ResultMetric label={t('submission.list.timeUsed')} value={formatOptionalDurationMs(judgeResult.timeUsedMs)} />
          <ResultMetric label={t('submission.list.spaceUsed')} value={formatOptionalMemoryKb(judgeResult.memoryUsedKb)} />
        </div>
        <JudgeReasonLine label={t('submission.detail.reason')} reason={judgeResult.reason} />

        {judgeResult.subtasks.map((subtask) => (
          <div key={subtask.index} className="rounded-lg border border-slate-200">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-4 py-3">
              <div>
                <p className="font-medium text-slate-950">{resultNodeTitle('subtask', subtask.index, subtask.label)}</p>
                <p className="text-sm text-slate-500">
                  {submissionVerdictLabel(subtask.verdict)} · {formatOptionalScore(subtask.score)}
                </p>
                <JudgeReasonLine label={t('submission.detail.reason')} reason={subtask.reason} />
              </div>
              <div className="text-sm text-slate-500">
                {formatOptionalDurationMs(subtask.timeUsedMs)} · {formatOptionalMemoryKb(subtask.memoryUsedKb)}
              </div>
            </div>
            {subtask.testcases.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[960px] text-left text-sm">
                  <thead className="bg-slate-50 text-slate-500">
                    <tr>
                      <th className="px-4 py-2 font-medium">{t('submission.detail.testcases')}</th>
                      <th className="px-4 py-2 font-medium">{t('common.verdict')}</th>
                      <th className="px-4 py-2 font-medium">{t('submission.list.score')}</th>
                      <th className="px-4 py-2 font-medium">{t('submission.list.timeUsed')}</th>
                      <th className="px-4 py-2 font-medium">{t('submission.list.spaceUsed')}</th>
                      <th className="px-4 py-2 font-medium">{t('submission.detail.reason')}</th>
                      <th className="px-4 py-2 font-medium">{t('submission.detail.checkerReport')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {subtask.testcases.map((testcase) => (
                      <tr key={testcase.index} className="border-t border-slate-100">
                        <td className="px-4 py-2 font-medium text-slate-900">{resultNodeTitle('testcase', testcase.index, testcase.label)}</td>
                        <td className="px-4 py-2 text-slate-700">
                          {submissionVerdictLabel(testcase.verdict)}
                        </td>
                        <td className="px-4 py-2 text-slate-700">{formatOptionalScore(testcase.score)}</td>
                        <td className="px-4 py-2 text-slate-700">{formatOptionalDurationMs(testcase.timeUsedMs)}</td>
                        <td className="px-4 py-2 text-slate-700">{formatOptionalMemoryKb(testcase.memoryUsedKb)}</td>
                        <td className="px-4 py-2 font-mono text-xs text-slate-600">{testcase.reason ?? '--'}</td>
                        <td className="max-w-[320px] whitespace-pre-wrap px-4 py-2 text-slate-700">
                          {testcase.message ?? '--'}
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
        ))}
      </CardContent>
    </Card>
  )
}

function resultNodeTitle(kind: 'subtask' | 'testcase', index: number, label: string | null): string {
  return label ? `${kind} ${index} (${label})` : `${kind} ${index}`
}

function ResultMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-slate-200 px-3 py-2">
      <p className="text-xs uppercase tracking-normal text-slate-500">{label}</p>
      <p className="mt-1 font-medium text-slate-950">{value}</p>
    </div>
  )
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
