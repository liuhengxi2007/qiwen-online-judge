import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { SubmissionPageModel } from '@/pages/hooks/submission/useSubmissionPageModel'
import { useI18n } from '@/system/i18n/use-i18n'
import { Files } from 'lucide-react'

import { ProblemFilterField } from './ProblemFilterField'
import { SortFilterField } from './SortFilterField'
import { SubmissionFilterSummary } from './SubmissionFilterSummary'
import { UserFilterField } from './UserFilterField'
import { VerdictFilterField } from './VerdictFilterField'

/**
 * 提交筛选卡片属性，直接接收提交列表页模型。
 */
type SubmissionFilterCardProps = {
  model: SubmissionPageModel
}

/**
 * 保留组件入口 props 解构：这里是路由页模型进入筛选卡片的唯一边界。
 */
export function SubmissionFilterCard({ model }: SubmissionFilterCardProps) {
  const { t } = useI18n()
  const userFilterInput = {
    value: model.usernameFilterInput,
    onValueChange: model.updateUsernameFilterInput,
    onFocusChange: model.setIsUsernameFilterFocused,
    onEnter: model.applyFiltersOnEnter,
  }
  const userSuggestions = {
    enabled: model.isUserSuggestionEnabled,
    isLoading: model.isLoadingUserSuggestions,
    isOpen: model.showUserSuggestionPanel,
    items: model.userSuggestions,
    onEnabledChange: model.setIsUserSuggestionEnabled,
    onSelect: model.selectUsernameSuggestion,
  }
  const problemFilterInput = {
    value: model.problemFilterInput,
    onValueChange: model.updateProblemFilterInput,
    onFocusChange: model.setIsProblemFilterFocused,
    onEnter: model.applyFiltersOnEnter,
  }
  const problemSuggestions = {
    enabled: model.isProblemSuggestionEnabled,
    isLoading: model.isLoadingProblemSuggestions,
    isOpen: model.showProblemSuggestionPanel,
    items: model.problemSuggestions,
    onEnabledChange: model.setIsProblemSuggestionEnabled,
    onSelect: model.selectProblemSuggestion,
  }

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
            model.hasFixedProblemFilter
              ? 'lg:grid-cols-[minmax(0,5fr)_minmax(0,4fr)_minmax(0,5fr)]'
              : 'lg:grid-cols-[minmax(0,5fr)_minmax(0,5fr)_minmax(0,4fr)_minmax(0,5fr)]'
          }`}
        >
          <UserFilterField input={userFilterInput} suggestions={userSuggestions} />

          {model.hasFixedProblemFilter ? null : <ProblemFilterField input={problemFilterInput} suggestions={problemSuggestions} />}

          <VerdictFilterField
            value={model.activeVerdictFilter}
            values={model.verdictFilterValues}
            label={model.verdictFilterLabel}
            onChange={model.updateVerdictFilter}
          />

          <SortFilterField
            state={{
              value: model.activeSort,
              values: model.submissionSortValues,
              direction: model.activeDirection,
            }}
            actions={{
              onChange: model.changeSort,
              onToggleDirection: model.toggleDirection,
            }}
          />
        </div>

        <div className="flex flex-wrap gap-3">
          <Button type="button" onClick={model.applyFilters}>
            {t('submission.filter.apply')}
          </Button>
          <Button type="button" variant="outline" onClick={model.clearFilters}>
            {t('submission.filter.clear')}
          </Button>
        </div>

        <SubmissionFilterSummary
          usernameQueryParam={model.usernameQueryParam}
          activeProblemQuery={model.activeProblemQuery}
          activeVerdictFilter={model.activeVerdictFilter}
          verdictFilterLabel={model.verdictFilterLabel}
        />
      </CardContent>
    </Card>
  )
}
