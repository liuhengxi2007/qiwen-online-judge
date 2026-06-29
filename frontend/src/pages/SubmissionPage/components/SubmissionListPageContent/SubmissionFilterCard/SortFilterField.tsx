import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { isSubmissionSort } from '@/objects/submission/request/SubmissionSort'
import type { SubmissionPageModel } from '../hooks/useSubmissionPageModel'
import { useI18n } from '@/system/i18n/use-i18n'

type SortFilterFieldProps = {
  state: {
    value: SubmissionPageModel['activeSort']
    values: SubmissionPageModel['submissionSortValues']
    direction: SubmissionPageModel['activeDirection']
  }
  actions: {
    onChange: SubmissionPageModel['changeSort']
    onToggleDirection: SubmissionPageModel['toggleDirection']
  }
}

/**
 * 保留子组件 props 解构：排序状态和动作已分组，子组件只展开这两个稳定边界。
 */
export function SortFilterField({ state, actions }: SortFilterFieldProps) {
  const { t } = useI18n()

  return (
    <div className="space-y-2">
      <Label htmlFor="submission-sort">{t('submission.filter.sort')}</Label>
      <div className="flex flex-wrap gap-2">
        <Select
          value={state.value}
          onValueChange={(nextValue) => {
            if (isSubmissionSort(nextValue)) {
              actions.onChange(nextValue)
            }
          }}
        >
          <SelectTrigger id="submission-sort" className="min-w-40 flex-1 rounded-2xl border-slate-300 bg-white">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {state.values.map((sort) => (
              <SelectItem key={sort} value={sort}>
                {t(`submission.sort.${sort}`)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button
          type="button"
          variant="outline"
          className="shrink-0"
          onClick={actions.onToggleDirection}
        >
          {state.direction === 'asc' ? t('submission.sort.ascending') : t('submission.sort.descending')}
        </Button>
      </div>
    </div>
  )
}
