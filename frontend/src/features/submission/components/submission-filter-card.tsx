import type { KeyboardEvent } from 'react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import type {
  SubmissionSort,
  SubmissionSortDirection,
  SubmissionVerdictFilter,
  ProblemSuggestion,
} from '@/features/submission/hooks/use-submission-page-model'
import type { UserIdentity } from '@/features/user/domain/user'
import { displayNameValue, usernameValue } from '@/features/user/domain/user'
import { problemSlugValue, problemTitleValue } from '@/features/problem/domain/problem'
import { useI18n } from '@/shared/i18n/i18n'
import { Files } from 'lucide-react'

type SubmissionFilterCardProps = {
  hasFixedProblemFilter: boolean
  usernameFilterInput: string
  problemFilterInput: string
  isUsernameFilterFocused: boolean
  isProblemFilterFocused: boolean
  isUserSuggestionEnabled: boolean
  isProblemSuggestionEnabled: boolean
  isLoadingUserSuggestions: boolean
  isLoadingProblemSuggestions: boolean
  showUserSuggestionPanel: boolean
  showProblemSuggestionPanel: boolean
  userSuggestions: UserIdentity[]
  problemSuggestions: ProblemSuggestion[]
  activeVerdictFilter: SubmissionVerdictFilter
  activeSort: SubmissionSort
  activeDirection: SubmissionSortDirection
  verdictFilterValues: readonly SubmissionVerdictFilter[]
  submissionSortValues: readonly SubmissionSort[]
  activeProblemQuery: string
  usernameQueryParam: string
  verdictFilterLabel: (verdict: SubmissionVerdictFilter, allVerdictsLabel: string) => string
  onUsernameFilterInputChange: (value: string) => void
  onProblemFilterInputChange: (value: string) => void
  onUsernameFocusChange: (focused: boolean) => void
  onProblemFocusChange: (focused: boolean) => void
  onUserSuggestionEnabledChange: (enabled: boolean) => void
  onProblemSuggestionEnabledChange: (enabled: boolean) => void
  onUsernameSuggestionSelect: (username: string) => void
  onProblemSuggestionSelect: (slug: string) => void
  onVerdictFilterChange: (value: SubmissionVerdictFilter) => void
  onSortChange: (value: SubmissionSort) => void
  onToggleDirection: () => void
  onApplyFilters: () => void
  onClearFilters: () => void
  onApplyFiltersOnEnter: (event: KeyboardEvent<HTMLInputElement>) => void
}

export function SubmissionFilterCard(props: SubmissionFilterCardProps) {
  const { t } = useI18n()
  const {
    hasFixedProblemFilter,
    usernameFilterInput,
    problemFilterInput,
    isLoadingUserSuggestions,
    isLoadingProblemSuggestions,
    showUserSuggestionPanel,
    showProblemSuggestionPanel,
    userSuggestions,
    problemSuggestions,
    activeVerdictFilter,
    activeSort,
    activeDirection,
    verdictFilterValues,
    submissionSortValues,
    activeProblemQuery,
    usernameQueryParam,
  } = props

  return (
    <Card className="mb-6 border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-indigo-100 text-indigo-700">
            <Files className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl text-slate-950">{t('submission.filter.title')}</CardTitle>
            <CardDescription>{t('submission.filter.description')}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div
          className={`grid gap-4 ${
            hasFixedProblemFilter
              ? 'lg:grid-cols-[minmax(0,5fr)_minmax(0,4fr)_minmax(0,5fr)]'
              : 'lg:grid-cols-[minmax(0,5fr)_minmax(0,5fr)_minmax(0,4fr)_minmax(0,5fr)]'
          }`}
        >
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-3">
              <Label htmlFor="submission-username-filter">{t('common.username')}</Label>
              <div className="flex items-center gap-2">
                <Label htmlFor="submission-user-suggestion-toggle" className="text-xs text-slate-500">
                  {t('submission.filter.toggleUserSearch')}
                </Label>
                <Switch
                  id="submission-user-suggestion-toggle"
                  checked={props.isUserSuggestionEnabled}
                  onCheckedChange={props.onUserSuggestionEnabledChange}
                />
              </div>
            </div>
            <Input
              id="submission-username-filter"
              className="min-w-0"
              value={usernameFilterInput}
              onChange={(event) => props.onUsernameFilterInputChange(event.target.value)}
              onFocus={() => props.onUsernameFocusChange(true)}
              onBlur={() => props.onUsernameFocusChange(false)}
              onKeyDown={props.onApplyFiltersOnEnter}
            />
            {showUserSuggestionPanel ? (
              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-2">
                {isLoadingUserSuggestions ? (
                  <p className="px-3 py-2 text-sm text-slate-500">{t('common.loading')}</p>
                ) : userSuggestions.length === 0 ? (
                  <p className="px-3 py-2 text-sm text-slate-500">{t('common.emptyData')}</p>
                ) : (
                  userSuggestions.map((suggestion) => (
                    <button
                      key={usernameValue(suggestion.username)}
                      type="button"
                      className="flex w-full flex-col rounded-xl px-3 py-2 text-left text-sm hover:bg-white"
                      onMouseDown={(event) => event.preventDefault()}
                      onClick={() => props.onUsernameSuggestionSelect(usernameValue(suggestion.username))}
                    >
                      <span className="font-medium text-slate-900">{displayNameValue(suggestion.displayName)}</span>
                      <span className="text-slate-500">{usernameValue(suggestion.username)}</span>
                    </button>
                  ))
                )}
              </div>
            ) : null}
          </div>

          {hasFixedProblemFilter ? null : (
            <div className="space-y-2">
              <div className="flex items-center justify-between gap-3">
                <Label htmlFor="submission-problem-filter">{t('submission.filter.problemSlug')}</Label>
                <div className="flex items-center gap-2">
                  <Label htmlFor="submission-problem-suggestion-toggle" className="text-xs text-slate-500">
                    {t('submission.filter.toggleProblemSearch')}
                  </Label>
                  <Switch
                    id="submission-problem-suggestion-toggle"
                    checked={props.isProblemSuggestionEnabled}
                    onCheckedChange={props.onProblemSuggestionEnabledChange}
                  />
                </div>
              </div>
              <Input
                id="submission-problem-filter"
                className="min-w-0"
                value={problemFilterInput}
                onChange={(event) => props.onProblemFilterInputChange(event.target.value)}
                onFocus={() => props.onProblemFocusChange(true)}
                onBlur={() => props.onProblemFocusChange(false)}
                onKeyDown={props.onApplyFiltersOnEnter}
              />
              {showProblemSuggestionPanel ? (
                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-2">
                  {isLoadingProblemSuggestions ? (
                    <p className="px-3 py-2 text-sm text-slate-500">{t('common.loading')}</p>
                  ) : problemSuggestions.length === 0 ? (
                    <p className="px-3 py-2 text-sm text-slate-500">{t('common.emptyData')}</p>
                  ) : (
                    problemSuggestions.map((suggestion) => (
                      <button
                        key={problemSlugValue(suggestion.slug)}
                        type="button"
                        className="flex w-full flex-col rounded-xl px-3 py-2 text-left text-sm hover:bg-white"
                        onMouseDown={(event) => event.preventDefault()}
                        onClick={() => props.onProblemSuggestionSelect(problemSlugValue(suggestion.slug))}
                      >
                        <span className="font-medium text-slate-900">{problemTitleValue(suggestion.title)}</span>
                        <span className="text-slate-500">{problemSlugValue(suggestion.slug)}</span>
                      </button>
                    ))
                  )}
                </div>
              ) : null}
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor="submission-verdict-filter">{t('submission.filter.verdict')}</Label>
            <Select value={activeVerdictFilter} onValueChange={(value) => props.onVerdictFilterChange(value as SubmissionVerdictFilter)}>
              <SelectTrigger id="submission-verdict-filter" className="min-w-32 rounded-2xl border-slate-300 bg-white">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {verdictFilterValues.map((verdict) => (
                  <SelectItem key={verdict} value={verdict}>
                    {props.verdictFilterLabel(verdict, t('submission.filter.allVerdicts'))}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="submission-sort">{t('submission.filter.sort')}</Label>
            <div className="flex flex-wrap gap-2">
              <Select value={activeSort} onValueChange={(value) => props.onSortChange(value as SubmissionSort)}>
                <SelectTrigger id="submission-sort" className="min-w-40 flex-1 rounded-2xl border-slate-300 bg-white">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {submissionSortValues.map((sort) => (
                    <SelectItem key={sort} value={sort}>
                      {t(`submission.sort.${sort}`)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Button
                type="button"
                variant="outline"
                className="shrink-0 rounded-2xl border-slate-300 bg-white"
                onClick={props.onToggleDirection}
              >
                {activeDirection === 'asc' ? t('submission.sort.ascending') : t('submission.sort.descending')}
              </Button>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap gap-3">
          <Button type="button" className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800" onClick={props.onApplyFilters}>
            {t('submission.filter.apply')}
          </Button>
          <Button type="button" variant="outline" className="rounded-2xl border-slate-300 bg-white" onClick={props.onClearFilters}>
            {t('submission.filter.clear')}
          </Button>
        </div>

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
            verdict: props.verdictFilterLabel(activeVerdictFilter, t('submission.filter.allVerdicts')),
          })}
        </p>
      </CardContent>
    </Card>
  )
}
