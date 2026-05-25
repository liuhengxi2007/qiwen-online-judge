import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  formatOptionalDurationMs,
  formatOptionalMemoryKb,
  formatOptionalScore,
} from '@/features/submission/lib/submission-display'
import { submissionVerdictLabel } from '@/features/submission/lib/submission-parsers'
import type { JudgeResult } from '@/features/submission/model/JudgeResult'
import { useI18n } from '@/shared/i18n/use-i18n'

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
        {judgeResult.subtasks.map((subtask) => (
          <div key={subtask.name} className="rounded-lg border border-slate-200">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-4 py-3">
              <div>
                <p className="font-medium text-slate-950">{subtask.name}</p>
                <p className="text-sm text-slate-500">
                  {submissionVerdictLabel(subtask.verdict)} · {formatOptionalScore(subtask.score)}
                </p>
              </div>
              <div className="text-sm text-slate-500">
                {formatOptionalDurationMs(subtask.timeUsedMs)} · {formatOptionalMemoryKb(subtask.memoryUsedKb)}
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[720px] text-left text-sm">
                <thead className="bg-slate-50 text-slate-500">
                  <tr>
                    <th className="px-4 py-2 font-medium">{t('submission.detail.testcases')}</th>
                    <th className="px-4 py-2 font-medium">{t('common.verdict')}</th>
                    <th className="px-4 py-2 font-medium">{t('submission.list.score')}</th>
                    <th className="px-4 py-2 font-medium">{t('submission.list.timeUsed')}</th>
                    <th className="px-4 py-2 font-medium">{t('submission.list.spaceUsed')}</th>
                  </tr>
                </thead>
                <tbody>
                  {subtask.testcases.map((testcase) => (
                    <tr key={testcase.name} className="border-t border-slate-100">
                      <td className="px-4 py-2 font-medium text-slate-900">{testcase.name}</td>
                      <td className="px-4 py-2 text-slate-700">
                        {submissionVerdictLabel(testcase.verdict)}
                      </td>
                      <td className="px-4 py-2 text-slate-700">{formatOptionalScore(testcase.score)}</td>
                      <td className="px-4 py-2 text-slate-700">{formatOptionalDurationMs(testcase.timeUsedMs)}</td>
                      <td className="px-4 py-2 text-slate-700">{formatOptionalMemoryKb(testcase.memoryUsedKb)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}
