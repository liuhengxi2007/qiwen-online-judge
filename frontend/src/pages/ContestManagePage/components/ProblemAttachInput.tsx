import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { RowAction } from '@/components/ui/row-action'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { problemTitleValue } from '@/objects/problem/ProblemTitle'
import type { ProblemSuggestion } from '@/objects/problem/response/ProblemSuggestion'
import { useI18n } from '@/system/i18n/use-i18n'

type ProblemAttachInputProps = {
  input: string
  isLoading: boolean
  suggestions: ProblemSuggestion[]
  isAttaching: boolean
  onInputChange: (value: string) => void
  onFocusChange: (focused: boolean) => void
  onSuggestionSelect: (suggestion: ProblemSuggestion) => void
  onAttach: () => void
}

/**
 * 保留扁平 props 解构：这是局部输入控件，输入、建议、焦点和提交动作都直接服务同一个附加题目流程。
 */
export function ProblemAttachInput({
  input,
  isLoading,
  suggestions,
  isAttaching,
  onInputChange,
  onFocusChange,
  onSuggestionSelect,
  onAttach,
}: ProblemAttachInputProps) {
  const { t } = useI18n()

  return (
    <div className="space-y-2">
      <Label htmlFor="contest-problem-search">{t('contest.detail.attachProblemInput')}</Label>
      <Input
        id="contest-problem-search"
        value={input}
        onChange={(event) => onInputChange(event.target.value)}
        onFocus={() => onFocusChange(true)}
      />
      {suggestions.length > 0 || isLoading ? (
        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-2">
          {isLoading ? (
            <p className="px-3 py-2 text-sm text-slate-500">{t('common.loading')}</p>
          ) : (
            suggestions.map((suggestion) => (
              <RowAction
                key={problemSlugValue(suggestion.slug)}
                size="compact"
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => onSuggestionSelect(suggestion)}
              >
                <span className="font-medium text-slate-900">{problemTitleValue(suggestion.title)}</span>
                <span className="text-slate-500">{problemSlugValue(suggestion.slug)}</span>
              </RowAction>
            ))
          )}
        </div>
      ) : null}
      <Button type="button" variant="create" disabled={isAttaching} onClick={onAttach}>
        {isAttaching ? t('contest.detail.attachingProblem') : t('contest.detail.attachProblem')}
      </Button>
    </div>
  )
}
