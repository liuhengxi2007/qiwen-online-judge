import { Navigate, useSearchParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { useRanklistQuery } from '@/pages/hooks/user/use-ranklist-query'
import { useSessionGuard } from '@/pages/hooks/auth/use-session-guard'
import { AppSectionBar } from '@/pages/components/auth/app-section-bar'
import { AncestorNavigation } from '@/pages/components/ancestor-navigation'
import { usePageTitle } from '@/pages/hooks/use-page-title'
import { useI18n } from '@/system/i18n/use-i18n'

import { AcceptedRanklistCard } from './components/AcceptedRanklistCard'
import { ContributionRanklistCard } from './components/ContributionRanklistCard'
import { normalizePage } from './functions/page-query'

export function RanklistPage() {
  const { t } = useI18n()
  usePageTitle(t('ranklist.pageTitle'))
  const [searchParams] = useSearchParams()
  const { session: user, navigationIntent } = useSessionGuard()
  const contributionPage = normalizePage(searchParams.get('contributionPage') ?? searchParams.get('page'))
  const acceptedPage = normalizePage(searchParams.get('acceptedPage'))
  const query = useRanklistQuery({ acceptedPage, contributionPage })
  const contributionResponse = query.contributionRanklist
  const acceptedResponse = query.acceptedRanklist
  const contributionPageSize = contributionResponse?.pageSize ?? 10
  const acceptedPageSize = acceptedResponse?.pageSize ?? 10
  const contributionTotalPages = contributionResponse ? Math.max(1, Math.ceil(contributionResponse.totalItems / contributionResponse.pageSize)) : 1
  const acceptedTotalPages = acceptedResponse ? Math.max(1, Math.ceil(acceptedResponse.totalItems / acceptedResponse.pageSize)) : 1

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return (
    <main className="min-h-screen bg-[radial-gradient(circle_at_top_left,#fef3c7_0,#f8fafc_36%,#eef2f7_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('ranklist.heading')}</h1>
          </div>

          <AncestorNavigation />
        </div>

        <AppSectionBar />

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

        <div className="grid gap-6 xl:grid-cols-2">
          <AcceptedRanklistCard
            acceptedPage={acceptedPage}
            contributionPage={contributionPage}
            errorMessage={query.acceptedRanklistLoadError}
            isLoading={query.isLoadingAcceptedRanklist}
            items={acceptedResponse?.items ?? []}
            pageSize={acceptedPageSize}
            totalPages={acceptedTotalPages}
          />

          <ContributionRanklistCard
            acceptedPage={acceptedPage}
            contributionPage={contributionPage}
            errorMessage={query.contributionRanklistLoadError}
            isLoading={query.isLoadingContributionRanklist}
            items={contributionResponse?.items ?? []}
            pageSize={contributionPageSize}
            totalPages={contributionTotalPages}
          />
        </div>
      </section>
    </main>
  )
}
