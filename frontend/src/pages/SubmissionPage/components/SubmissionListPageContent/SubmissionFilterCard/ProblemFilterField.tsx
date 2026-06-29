import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { RowAction } from '@/components/ui/row-action'
import { Switch } from '@/components/ui/switch'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { problemTitleValue } from '@/objects/problem/ProblemTitle'
import { useI18n } from '@/system/i18n/use-i18n'

import type { ProblemSuggestionState, TextFilterInputState } from './objects/FilterFieldState'

type ProblemFilterFieldProps = {
  input: TextFilterInputState
  suggestions: ProblemSuggestionState
}

/**
 * 保留子组件 props 解构：组件边界已按 input/suggestions 分组，展开后 JSX 绑定更清楚。
 */
export function ProblemFilterField({ input, suggestions }: ProblemFilterFieldProps) {
  const { t } = useI18n()

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between gap-3">
        <Label htmlFor="submission-problem-filter">{t('submission.filter.problemSlug')}</Label>
        <div className="flex items-center gap-2">
          <Label htmlFor="submission-problem-suggestion-toggle" className="text-xs text-slate-500">
            {t('submission.filter.toggleProblemSearch')}
          </Label>
          <Switch
            id="submission-problem-suggestion-toggle"
            checked={suggestions.enabled}
            onCheckedChange={suggestions.onEnabledChange}
          />
        </div>
      </div>
      <Input
        id="submission-problem-filter"
        className="min-w-0"
        value={input.value}
        onChange={(event) => input.onValueChange(event.target.value)}
        onFocus={() => input.onFocusChange(true)}
        onBlur={() => input.onFocusChange(false)}
        onKeyDown={input.onEnter}
      />
      {suggestions.isOpen ? <ProblemSuggestionPanel suggestions={suggestions} /> : null}
    </div>
  )
}

/**
 * 保留内部面板 props 解构：建议列表面板只消费 suggestion 状态分组。
 */
function ProblemSuggestionPanel({ suggestions }: { suggestions: ProblemSuggestionState }) {
  const { t } = useI18n()

  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-2">
      {suggestions.isLoading ? (
        <p className="px-3 py-2 text-sm text-slate-500">{t('common.loading')}</p>
      ) : suggestions.items.length === 0 ? (
        <p className="px-3 py-2 text-sm text-slate-500">{t('common.emptyData')}</p>
      ) : (
        suggestions.items.map((suggestion) => (
          <RowAction
            key={problemSlugValue(suggestion.slug)}
            size="compact"
            onMouseDown={(event) => event.preventDefault()}
            onClick={() => suggestions.onSelect(problemSlugValue(suggestion.slug))}
          >
            <span className="font-medium text-slate-900">{problemTitleValue(suggestion.title)}</span>
            <span className="text-slate-500">{problemSlugValue(suggestion.slug)}</span>
          </RowAction>
        ))
      )}
    </div>
  )
}
