import { Navigate, useSearchParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { useRanklistQuery } from './hooks/useRanklistQuery'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { PageShell } from '@/pages/components/PageShell'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'

import { AcceptedRanklistCard } from './components/AcceptedRanklistCard'
import { ContributionRanklistCard } from './components/ContributionRanklistCard'
import { RatingRanklistCard } from './components/RatingRanklistCard'
import { normalizePage } from './functions/PageQuery'

export function RanklistPage() {
  const { t } = useI18n()
  usePageTitle(t('ranklist.pageTitle'))
  const [searchParams] = useSearchParams()
  const { session: user, navigationIntent } = useSessionGuard()
  const contributionPage = normalizePage(searchParams.get('contributionPage') ?? searchParams.get('page'))
  const acceptedPage = normalizePage(searchParams.get('acceptedPage'))
  const ratingPage = normalizePage(searchParams.get('ratingPage'))
  const query = useRanklistQuery({ acceptedPage, contributionPage, ratingPage })
  const contributionResponse = query.contributionRanklist
  const acceptedResponse = query.acceptedRanklist
  const ratingResponse = query.ratingRanklist
  const contributionPageSize = contributionResponse?.pageSize ?? 10
  const acceptedPageSize = acceptedResponse?.pageSize ?? 10
  const ratingPageSize = ratingResponse?.pageSize ?? 10
  const contributionTotalPages = contributionResponse ? Math.max(1, Math.ceil(contributionResponse.totalItems / contributionResponse.pageSize)) : 1
  const acceptedTotalPages = acceptedResponse ? Math.max(1, Math.ceil(acceptedResponse.totalItems / acceptedResponse.pageSize)) : 1
  const ratingTotalPages = ratingResponse ? Math.max(1, Math.ceil(ratingResponse.totalItems / ratingResponse.pageSize)) : 1

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return (
    <PageShell
      title={t('ranklist.heading')}
      mainClassName="bg-[radial-gradient(circle_at_top_left,#fef3c7_0,#f8fafc_36%,#eef2f7_100%)]"
    >
      {query.contributionRanklistLoadError ? (
        <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{query.contributionRanklistLoadError}</AlertDescription>
        </Alert>
      ) : null}

      {query.acceptedRanklistLoadError ? (
        <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{query.acceptedRanklistLoadError}</AlertDescription>
        </Alert>
      ) : null}

      {query.ratingRanklistLoadError ? (
        <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{query.ratingRanklistLoadError}</AlertDescription>
        </Alert>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-3">
        <RatingRanklistCard
          acceptedPage={acceptedPage}
          contributionPage={contributionPage}
          errorMessage={query.ratingRanklistLoadError}
          isLoading={query.isLoadingRatingRanklist}
          items={ratingResponse?.items ?? []}
          pageSize={ratingPageSize}
          ratingPage={ratingPage}
          totalPages={ratingTotalPages}
        />

        <AcceptedRanklistCard
          acceptedPage={acceptedPage}
          contributionPage={contributionPage}
          errorMessage={query.acceptedRanklistLoadError}
          isLoading={query.isLoadingAcceptedRanklist}
          items={acceptedResponse?.items ?? []}
          pageSize={acceptedPageSize}
          ratingPage={ratingPage}
          totalPages={acceptedTotalPages}
        />

        <ContributionRanklistCard
          acceptedPage={acceptedPage}
          contributionPage={contributionPage}
          errorMessage={query.contributionRanklistLoadError}
          isLoading={query.isLoadingContributionRanklist}
          items={contributionResponse?.items ?? []}
          pageSize={contributionPageSize}
          ratingPage={ratingPage}
          totalPages={contributionTotalPages}
        />
      </div>
    </PageShell>
  )
}
