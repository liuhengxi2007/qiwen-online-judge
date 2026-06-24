import { Navigate } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { PageShell } from '@/pages/components/PageShell'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { useI18n } from '@/system/i18n/use-i18n'
import { useSubmissionPageModel } from '@/pages/hooks/submission/useSubmissionPageModel'
import { SubmissionFilterCard } from './SubmissionFilterCard'
import { SubmissionSummaryList } from './SubmissionSummaryList'

/**
 * 提交列表页面内容属性，允许固定题目筛选或限定在比赛范围内查询。
 */
type SubmissionListPageContentProps = {
  fixedProblemSlugFilter?: ProblemSlug
  contestSlug?: ContestSlug
  titleKey?: string
}

/**
 * 提交列表页面主体，负责会话守卫、筛选模型、错误提示、筛选卡片和结果列表组合。
 */
export function SubmissionListPageContent({ fixedProblemSlugFilter, contestSlug, titleKey = 'submission.heading' }: SubmissionListPageContentProps = {}) {
  const { t } = useI18n()
  usePageTitle(t('submission.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const model = useSubmissionPageModel(fixedProblemSlugFilter, contestSlug)

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return (
    <PageShell title={t(titleKey)} mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#edf4fb_100%)]">
      {model.submissionQuery.errorMessage ? (
        <Alert variant="destructive" className="mb-6">
          <AlertDescription>{model.submissionQuery.errorMessage}</AlertDescription>
        </Alert>
      ) : null}

      <SubmissionFilterCard
        hasFixedProblemFilter={model.hasFixedProblemFilter}
        usernameFilterInput={model.usernameFilterInput}
        problemFilterInput={model.problemFilterInput}
        isUsernameFilterFocused={model.isUsernameFilterFocused}
        isProblemFilterFocused={model.isProblemFilterFocused}
        isUserSuggestionEnabled={model.isUserSuggestionEnabled}
        isProblemSuggestionEnabled={model.isProblemSuggestionEnabled}
        isLoadingUserSuggestions={model.isLoadingUserSuggestions}
        isLoadingProblemSuggestions={model.isLoadingProblemSuggestions}
        showUserSuggestionPanel={model.showUserSuggestionPanel}
        showProblemSuggestionPanel={model.showProblemSuggestionPanel}
        userSuggestions={model.userSuggestions}
        problemSuggestions={model.problemSuggestions}
        activeVerdictFilter={model.activeVerdictFilter}
        activeSort={model.activeSort}
        activeDirection={model.activeDirection}
        verdictFilterValues={model.verdictFilterValues}
        submissionSortValues={model.submissionSortValues}
        activeProblemQuery={model.activeProblemQuery}
        usernameQueryParam={model.usernameQueryParam}
        verdictFilterLabel={model.verdictFilterLabel}
        onUsernameFilterInputChange={model.updateUsernameFilterInput}
        onProblemFilterInputChange={model.updateProblemFilterInput}
        onUsernameFocusChange={model.setIsUsernameFilterFocused}
        onProblemFocusChange={model.setIsProblemFilterFocused}
        onUserSuggestionEnabledChange={model.setIsUserSuggestionEnabled}
        onProblemSuggestionEnabledChange={model.setIsProblemSuggestionEnabled}
        onUsernameSuggestionSelect={model.selectUsernameSuggestion}
        onProblemSuggestionSelect={model.selectProblemSuggestion}
        onVerdictFilterChange={model.updateVerdictFilter}
        onSortChange={model.changeSort}
        onToggleDirection={model.toggleDirection}
        onApplyFilters={model.applyFilters}
        onClearFilters={model.clearFilters}
        onApplyFiltersOnEnter={model.applyFiltersOnEnter}
      />

      <SubmissionSummaryList
        submissions={model.currentPageSubmissions}
        viewer={user}
        isLoading={model.submissionQuery.isLoading}
        currentPage={model.currentPage}
        totalPages={model.totalPages}
        pageNumbers={model.pageNumbers}
        onPageChange={model.goToPage}
      />
    </PageShell>
  )
}
