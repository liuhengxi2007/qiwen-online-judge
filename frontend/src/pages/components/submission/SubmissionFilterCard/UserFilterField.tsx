import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { RowAction } from '@/components/ui/row-action'
import { Switch } from '@/components/ui/switch'
import { displayNameValue } from '@/objects/user/DisplayName'
import { usernameValue } from '@/objects/user/Username'
import { useI18n } from '@/system/i18n/use-i18n'

import type { TextFilterInputState, UserSuggestionState } from './types'

type UserFilterFieldProps = {
  input: TextFilterInputState
  suggestions: UserSuggestionState
}

/**
 * 保留子组件 props 解构：组件边界已按 input/suggestions 分组，展开后 JSX 绑定更清楚。
 */
export function UserFilterField({ input, suggestions }: UserFilterFieldProps) {
  const { t } = useI18n()

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between gap-3">
        <Label htmlFor="submission-username-filter">{t('common.username')}</Label>
        <div className="flex items-center gap-2">
          <Label htmlFor="submission-user-suggestion-toggle" className="text-xs text-slate-500">
            {t('submission.filter.toggleUserSearch')}
          </Label>
          <Switch
            id="submission-user-suggestion-toggle"
            checked={suggestions.enabled}
            onCheckedChange={suggestions.onEnabledChange}
          />
        </div>
      </div>
      <Input
        id="submission-username-filter"
        className="min-w-0"
        value={input.value}
        onChange={(event) => input.onValueChange(event.target.value)}
        onFocus={() => input.onFocusChange(true)}
        onBlur={() => input.onFocusChange(false)}
        onKeyDown={input.onEnter}
      />
      {suggestions.isOpen ? <UserSuggestionPanel suggestions={suggestions} /> : null}
    </div>
  )
}

/**
 * 保留内部面板 props 解构：建议列表面板只消费 suggestion 状态分组。
 */
function UserSuggestionPanel({ suggestions }: { suggestions: UserSuggestionState }) {
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
            key={usernameValue(suggestion.username)}
            size="compact"
            onMouseDown={(event) => event.preventDefault()}
            onClick={() => suggestions.onSelect(usernameValue(suggestion.username))}
          >
            <span className="font-medium text-slate-900">{displayNameValue(suggestion.displayName)}</span>
            <span className="text-slate-500">{usernameValue(suggestion.username)}</span>
          </RowAction>
        ))
      )}
    </div>
  )
}
