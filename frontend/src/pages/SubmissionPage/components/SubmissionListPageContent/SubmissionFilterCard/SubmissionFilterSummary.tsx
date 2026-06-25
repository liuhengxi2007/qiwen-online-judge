import type { SubmissionPageModel } from '../hooks/useSubmissionPageModel'
import { useI18n } from '@/system/i18n/use-i18n'

type SubmissionFilterSummaryProps = {
  usernameQueryParam: string
  activeProblemQuery: string
  activeVerdictFilter: SubmissionPageModel['activeVerdictFilter']
  verdictFilterLabel: SubmissionPageModel['verdictFilterLabel']
}

/**
 * 保留子组件 props 解构：摘要只消费展示字段，单独展开能避免把完整 page model 传入纯展示组件。
 */
export function SubmissionFilterSummary({
  usernameQueryParam,
  activeProblemQuery,
  activeVerdictFilter,
  verdictFilterLabel,
}: SubmissionFilterSummaryProps) {
  const { t } = useI18n()

  return (
    <>
      {usernameQueryParam ? (
        <p className="text-sm text-slate-600">
          {t('submission.filter.showingUser', { query: usernameQueryParam })}
        </p>
      ) : (
        <p className="text-sm text-slate-600">{t('submission.filter.showingAll')}</p>
      )}
      <p className="text-sm text-slate-600">
        {t('submission.filter.activeSummary', {
          problem: activeProblemQuery || t('submission.filter.anyProblem'),
          verdict: verdictFilterLabel(activeVerdictFilter, t('submission.filter.allVerdicts')),
        })}
      </p>
    </>
  )
}
