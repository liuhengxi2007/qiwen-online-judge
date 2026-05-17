import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { Medal, Trophy } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { contributionTextClassName } from '@/features/auth/domain/contribution-style'
import { userContributionValue, type UserAcceptedRanklistItem, type UserRanklistItem } from '@/features/auth/domain/auth'
import { useRanklistQuery } from '@/features/auth/hooks/use-ranklist-query'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { AppSectionBar } from '@/shared/components/app-section-bar'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { UserProfileLink } from '@/shared/components/user-profile-link'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/use-i18n'

function normalizePage(rawPage: string | null): number {
  const parsedPage = rawPage ? Number.parseInt(rawPage, 10) : 1
  return Number.isFinite(parsedPage) && parsedPage > 0 ? parsedPage : 1
}

function pagePath(contributionPage: number, acceptedPage: number): string {
  return `/ranklist?contributionPage=${contributionPage}&acceptedPage=${acceptedPage}`
}

type RanklistPaginationProps = {
  acceptedPage: number
  contributionPage: number
  currentPage: number
  totalPages: number
  target: 'accepted' | 'contribution'
}

function RanklistPagination({
  acceptedPage,
  contributionPage,
  currentPage,
  totalPages,
  target,
}: RanklistPaginationProps) {
  const { t } = useI18n()
  const canGoPrevious = currentPage > 1
  const canGoNext = currentPage < totalPages
  const previousContributionPage = target === 'contribution' ? currentPage - 1 : contributionPage
  const previousAcceptedPage = target === 'accepted' ? currentPage - 1 : acceptedPage
  const nextContributionPage = target === 'contribution' ? currentPage + 1 : contributionPage
  const nextAcceptedPage = target === 'accepted' ? currentPage + 1 : acceptedPage

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
      <p className="text-sm text-slate-500">
        {t('ranklist.pageStatus', { page: String(currentPage), totalPages: String(totalPages) })}
      </p>
      <div className="flex gap-2">
        <Button asChild={canGoPrevious} disabled={!canGoPrevious} variant="outline" className="rounded-2xl">
          {canGoPrevious ? (
            <Link to={pagePath(previousContributionPage, previousAcceptedPage)}>{t('submission.pagination.previous')}</Link>
          ) : (
            <span>{t('submission.pagination.previous')}</span>
          )}
        </Button>
        <Button asChild={canGoNext} disabled={!canGoNext} variant="outline" className="rounded-2xl">
          {canGoNext ? (
            <Link to={pagePath(nextContributionPage, nextAcceptedPage)}>{t('submission.pagination.next')}</Link>
          ) : (
            <span>{t('submission.pagination.next')}</span>
          )}
        </Button>
      </div>
    </div>
  )
}

type ContributionRanklistCardProps = {
  acceptedPage: number
  contributionPage: number
  errorMessage: string
  items: UserRanklistItem[]
  isLoading: boolean
  pageSize: number
  totalPages: number
}

function ContributionRanklistCard({
  acceptedPage,
  contributionPage,
  errorMessage,
  items,
  isLoading,
  pageSize,
  totalPages,
}: ContributionRanklistCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white/95 shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <CardTitle className="text-xl text-slate-950">{t('ranklist.contributionTitle')}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4 p-5 pt-0 sm:p-6 sm:pt-0">
        {isLoading ? (
          <p className="text-sm text-slate-500">{t('ranklist.loading')}</p>
        ) : errorMessage ? (
          <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center">
            <p className="text-base font-medium text-slate-900">{t('ranklist.unavailable')}</p>
          </div>
        ) : items.length > 0 ? (
          items.map((item, index) => {
            const rank = (contributionPage - 1) * pageSize + index + 1
            const contribution = Math.round(userContributionValue(item.contribution))
            return (
              <div
                key={item.user.username}
                className="grid gap-4 rounded-3xl border border-slate-200 bg-slate-50 p-5 sm:grid-cols-[5rem_1fr_9rem] sm:items-center"
              >
                <div className="flex items-center gap-2 text-slate-700">
                  {rank <= 3 ? <Trophy className="size-4 text-amber-600" /> : <Medal className="size-4 text-slate-400" />}
                  <span className="text-lg font-semibold">#{rank}</span>
                </div>
                <UserProfileLink user={item.user} />
                <div className="text-left sm:text-right">
                  <p className="text-xs uppercase tracking-[0.18em] text-slate-400">{t('ranklist.contribution')}</p>
                  <p className={`mt-1 text-2xl font-semibold ${contributionTextClassName(contribution)}`}>{contribution}</p>
                </div>
              </div>
            )
          })
        ) : (
          <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center">
            <p className="text-base font-medium text-slate-900">{t('ranklist.empty')}</p>
          </div>
        )}

        {!isLoading && !errorMessage && items.length > 0 ? (
          <RanklistPagination
            acceptedPage={acceptedPage}
            contributionPage={contributionPage}
            currentPage={contributionPage}
            target="contribution"
            totalPages={totalPages}
          />
        ) : null}
      </CardContent>
    </Card>
  )
}

type AcceptedRanklistCardProps = {
  acceptedPage: number
  contributionPage: number
  errorMessage: string
  items: UserAcceptedRanklistItem[]
  isLoading: boolean
  pageSize: number
  totalPages: number
}

function AcceptedRanklistCard({
  acceptedPage,
  contributionPage,
  errorMessage,
  items,
  isLoading,
  pageSize,
  totalPages,
}: AcceptedRanklistCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white/95 shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <CardTitle className="text-xl text-slate-950">{t('ranklist.acceptedTitle')}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4 p-5 pt-0 sm:p-6 sm:pt-0">
        {isLoading ? (
          <p className="text-sm text-slate-500">{t('ranklist.loading')}</p>
        ) : errorMessage ? (
          <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center">
            <p className="text-base font-medium text-slate-900">{t('ranklist.unavailable')}</p>
          </div>
        ) : items.length > 0 ? (
          items.map((item, index) => {
            const rank = (acceptedPage - 1) * pageSize + index + 1
            return (
              <div
                key={item.user.username}
                className="grid gap-4 rounded-3xl border border-slate-200 bg-slate-50 p-5 sm:grid-cols-[5rem_1fr_9rem] sm:items-center"
              >
                <div className="flex items-center gap-2 text-slate-700">
                  {rank <= 3 ? <Trophy className="size-4 text-amber-600" /> : <Medal className="size-4 text-slate-400" />}
                  <span className="text-lg font-semibold">#{rank}</span>
                </div>
                <UserProfileLink user={item.user} />
                <div className="text-left sm:text-right">
                  <p className="text-xs uppercase tracking-[0.18em] text-slate-400">{t('ranklist.acceptedCount')}</p>
                  <p className="mt-1 text-2xl font-semibold text-emerald-700">{item.acceptedCount}</p>
                </div>
              </div>
            )
          })
        ) : (
          <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center">
            <p className="text-base font-medium text-slate-900">{t('ranklist.empty')}</p>
          </div>
        )}

        {!isLoading && !errorMessage && items.length > 0 ? (
          <RanklistPagination
            acceptedPage={acceptedPage}
            contributionPage={contributionPage}
            currentPage={acceptedPage}
            target="accepted"
            totalPages={totalPages}
          />
        ) : null}
      </CardContent>
    </Card>
  )
}

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
