import { Navigate } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { PageShell } from '@/pages/components/PageShell'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { useI18n } from '@/system/i18n/use-i18n'
import { SubmissionFilterCard } from './components/SubmissionFilterCard'
import { SubmissionSummaryList } from './components/SubmissionSummaryList'
import { useSubmissionPageModel } from './hooks/useSubmissionPageModel'

type SubmissionListPageContentProps = {
  fixedProblemSlugFilter?: ProblemSlug
  contestSlug?: ContestSlug
  titleKey?: string
}

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
        <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{model.submissionQuery.errorMessage}</AlertDescription>
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
