import type { JudgeSubtaskResult } from '@/objects/submission/JudgeSubtaskResult'
import { scoreTextStyleForRatio } from '@/pages/objects/ScoreDisplay'
import {
  formatOptionalDurationMs,
  formatOptionalMemoryKb,
  formatOptionalScore,
  formatTestcaseDetail,
  submissionVerdictLabel,
  submissionVerdictTextStyle,
} from '@/pages/objects/SubmissionDisplay'
import { useI18n } from '@/system/i18n/use-i18n'

import { resultNodeTitle } from './resultNodeTitle'

/**
 * 子任务测试点表格，展示每个测试点的 verdict、分数、耗时、内存和详细原因。
 */
export function SubtaskTestcaseTable({ subtask }: { subtask: JudgeSubtaskResult }) {
  const { t } = useI18n()

  if (subtask.testcases.length === 0) {
    return <p className="px-4 py-3 text-sm text-slate-500">{t('submission.detail.noTestcases')}</p>
  }

  return (
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
  )
}
