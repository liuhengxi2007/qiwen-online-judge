import { Navigate } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { AncestorNavigation } from '@/pages/components/ancestor-navigation'
import { AppSectionBar } from '@/pages/components/app-section-bar'
import { usePageTitle } from '@/pages/hooks/use-page-title'
import { useSessionGuard } from '@/pages/hooks/use-session-guard'
import { useI18n } from '@/system/i18n/use-i18n'
import { SubmissionFilterCard } from './components/submission-filter-card'
import { SubmissionSummaryList } from './components/submission-summary-list'
import { useSubmissionPageModel } from './hooks/use-submission-page-model'

type SubmissionListPageContentProps = {
  fixedProblemSlugFilter?: ProblemSlug
}

export function SubmissionListPageContent({ fixedProblemSlugFilter }: SubmissionListPageContentProps = {}) {
  const { t } = useI18n()
  usePageTitle(t('submission.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const model = useSubmissionPageModel(fixedProblemSlugFilter)

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#edf4fb_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('submission.heading')}</h1>
          </div>
          <AncestorNavigation />
        </div>

        <AppSectionBar />

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
      </section>
    </main>
  )
}
