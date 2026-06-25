import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { isSubmissionVerdictFilter } from '@/objects/submission/request/SubmissionVerdictFilter'
import type { SubmissionPageModel } from '@/pages/hooks/submission/useSubmissionPageModel'
import { useI18n } from '@/system/i18n/use-i18n'

type VerdictFilterFieldProps = {
  value: SubmissionPageModel['activeVerdictFilter']
  values: SubmissionPageModel['verdictFilterValues']
  label: SubmissionPageModel['verdictFilterLabel']
  onChange: SubmissionPageModel['updateVerdictFilter']
}

/**
 * 保留子组件 props 解构：verdict 字段本身就是该控件的完整状态边界。
 */
export function VerdictFilterField({
  value,
  values,
  label,
  onChange,
}: VerdictFilterFieldProps) {
  const { t } = useI18n()

  return (
    <div className="space-y-2">
      <Label htmlFor="submission-verdict-filter">{t('submission.filter.verdict')}</Label>
      <Select
        value={value}
        onValueChange={(nextValue) => {
          if (isSubmissionVerdictFilter(nextValue)) {
            onChange(nextValue)
          }
        }}
      >
        <SelectTrigger id="submission-verdict-filter" className="min-w-32 rounded-2xl border-slate-300 bg-white">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {values.map((verdict) => (
            <SelectItem key={verdict} value={verdict}>
              {label(verdict, t('submission.filter.allVerdicts'))}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  )
}
