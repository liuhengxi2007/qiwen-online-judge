import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { CalendarDays, CalendarPlus } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { contestDescriptionValue } from '@/objects/contest/ContestDescription'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import { contestTitleValue } from '@/objects/contest/ContestTitle'
import { DateTimeText } from '@/pages/components/DateTimeText'
import { PageShell } from '@/pages/components/PageShell'
import { PaginationControls } from '@/pages/components/PaginationControls'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { usePageSearchParamCorrection } from '@/pages/hooks/usePageSearchParamCorrection'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { buildPageNumbers, calculateTotalPages, parsePositivePage } from '@/pages/objects/Pagination'
import { resourceAccessBadgeLabel } from '@/pages/objects/ResourceAccessDisplay'
import { useI18n } from '@/system/i18n/use-i18n'
import { useContestPageModel } from './hooks/useContestPageModel'
import { useNow } from './hooks/useNow'

const contestsPerPage = 10

/**
 * 比赛列表页，负责会话守卫、分页查询、创建入口和列表展示。
 */
export function ContestPage() {
  const { t } = useI18n()
  usePageTitle(t('contest.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const [searchParams, setSearchParams] = useSearchParams()
  const currentPage = parsePositivePage(searchParams.get('page'))
  const model = useContestPageModel({ page: currentPage, pageSize: contestsPerPage })
  const now = useNow()
  const totalPages = calculateTotalPages(model.totalItems, model.pageSize)
  const pageNumbers = buildPageNumbers(currentPage, totalPages)

  usePageSearchParamCorrection({
    currentPage,
    totalPages,
    isLoading: model.isLoading,
    setSearchParams,
  })

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const canCreate = user.contestManager
  const onPageChange = (page: number) => {
    const nextSearchParams = new URLSearchParams(searchParams)
    nextSearchParams.set('page', String(page))
    nextSearchParams.delete('created')
    setSearchParams(nextSearchParams)
  }

  return (
    <PageShell title={t('contest.heading')} mainClassName="bg-[linear-gradient(180deg,#f0fdfa_0%,#ecfeff_48%,#f8fafc_100%)]">
      <div className="space-y-6">
        {model.errorMessage ? (
          <Alert variant="destructive">
            <AlertDescription>{model.errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {searchParams.get('created') ? (
          <Alert variant="success">
            <AlertDescription>{t('contest.list.created')}</AlertDescription>
          </Alert>
        ) : null}

        {model.registrationMessage ? (
          <Alert variant="success">
            <AlertDescription>{model.registrationMessage}</AlertDescription>
          </Alert>
        ) : null}

        <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
          <CardHeader>
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
                  <CalendarDays className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">{t('contest.list.cardTitle')}</CardTitle>
                  <CardDescription>{t('contest.list.cardDescription')}</CardDescription>
                </div>
              </div>
              {canCreate ? (
                <Button asChild variant="create">
                  <Link to="/contests/new">
                    <CalendarPlus className="size-4" />
                    {t('contest.list.create')}
                  </Link>
                </Button>
              ) : null}
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {model.isLoading ? (
              <p className="text-sm text-slate-500">{t('contest.list.loading')}</p>
            ) : model.contests.length === 0 ? (
              <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center">
                <p className="text-base font-medium text-slate-900">{t('contest.list.emptyTitle')}</p>
                <p className="mt-2 text-sm leading-7 text-slate-600">{t('contest.list.emptyDescription')}</p>
              </div>
            ) : (
              model.contests.map((contest) => {
                const hasStarted = now >= new Date(contest.startAt).getTime()
                const isRegistered = contest.registrationStatus.isRegistered
                const isUpdating = model.activeRegistrationSlug === contest.slug
                const isLocked = hasStarted
                const contestPath = `/contests/${contestSlugValue(contest.slug)}`
                return (
                  <div key={contest.id} className="rounded-3xl border border-slate-200 bg-slate-50 p-5">
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                      <div className="flex flex-wrap items-center gap-3">
                        {contest.canViewDetail ? (
                          <Link className="text-lg font-semibold text-slate-950 hover:underline" to={contestPath}>
                            {contestTitleValue(contest.title)}
                          </Link>
                        ) : (
                          <span className="text-lg font-semibold text-slate-500">{contestTitleValue(contest.title)}</span>
                        )}
                        <Badge variant="secondary">{resourceAccessBadgeLabel(contest.accessPolicy, t)}</Badge>
                        {!contest.canViewDetail ? <Badge variant="outline">{t('contest.list.detailUnavailable')}</Badge> : null}
                      </div>
                      <Button
                        type="button"
                        disabled={isUpdating || isLocked}
                        variant={isRegistered && !hasStarted ? 'destructiveOutline' : isRegistered || hasStarted ? 'outline' : 'default'}
                        className={
                          isRegistered && hasStarted
                            ? 'rounded-2xl border-emerald-200 bg-white text-emerald-700 hover:bg-white hover:text-emerald-700'
                            : hasStarted
                              ? 'rounded-2xl border-slate-200 bg-white text-slate-500 hover:bg-white hover:text-slate-500'
                              : undefined
                        }
                        onClick={() => {
                          if (!isLocked) {
                            void model.toggleRegistration(contest)
                          }
                        }}
                      >
                        {isUpdating
                          ? t('contest.list.registrationUpdating')
                          : isRegistered && hasStarted
                            ? t('contest.list.registered')
                            : isRegistered
                              ? t('contest.list.unregister')
                            : hasStarted
                              ? t('contest.list.registrationClosed')
                              : t('contest.list.register')}
                      </Button>
                    </div>
                    <p className="mt-2 font-mono text-sm text-slate-500">{contestSlugValue(contest.slug)}</p>
                    <p className="mt-3 text-sm leading-7 text-slate-600">
                      {contestDescriptionValue(contest.description) || t('common.noDescription')}
                    </p>
                    <div className="mt-4 grid gap-3 text-sm text-slate-600 sm:grid-cols-2">
                      <p>
                        <span className="font-medium text-slate-900">{t('contest.list.startAt')} </span>
                        <DateTimeText value={contest.startAt} />
                      </p>
                      <p>
                        <span className="font-medium text-slate-900">{t('contest.list.endAt')} </span>
                        <DateTimeText value={contest.endAt} />
                      </p>
                    </div>
                    <p className="mt-4 text-xs uppercase tracking-[0.18em] text-slate-400">
                      <span>{t('common.authorLabel')} </span>
                      {contest.author ? (
                        <UserProfileLink
                          className="inline-flex items-baseline gap-2 normal-case tracking-normal"
                          showUsername
                          user={contest.author}
                        />
                      ) : (
                        <span className="normal-case tracking-normal">{t('common.noAuthor')}</span>
                      )}
                    </p>
                  </div>
                )
              })
            )}
            {!model.isLoading && model.contests.length > 0 && totalPages > 1 ? (
              <PaginationControls
                currentPage={currentPage}
                pageNumbers={pageNumbers}
                totalPages={totalPages}
                previousLabel={t('common.pagination.previous')}
                nextLabel={t('common.pagination.next')}
                onPageChange={onPageChange}
              />
            ) : null}
          </CardContent>
        </Card>
      </div>
    </PageShell>
  )
}
