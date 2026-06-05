import type { CSSProperties } from 'react'
import { Link, Navigate, useParams, useSearchParams } from 'react-router-dom'
import { Trophy } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { contestProblemAliasValue } from '@/objects/contest/ContestProblemAlias'
import { contestPenaltyMillisValue } from '@/objects/contest/ContestPenaltyMillis'
import { contestRankValue } from '@/objects/contest/ContestRank'
import { contestScoreValue } from '@/objects/contest/ContestScore'
import { parseContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestRanklistItem } from '@/objects/contest/response/ContestRanklistItem'
import type { ContestRanklistProblemResult } from '@/objects/contest/response/ContestRanklistProblemResult'
import { problemTitleValue } from '@/objects/problem/ProblemTitle'
import { submissionIdValue } from '@/objects/submission/SubmissionId'
import { PageShell } from '@/pages/components/PageShell'
import { PaginationControls } from '@/pages/components/PaginationControls'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { usePageSearchParamCorrection } from '@/pages/hooks/usePageSearchParamCorrection'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { buildPageNumbers, calculateTotalPages, parsePositivePage } from '@/pages/objects/Pagination'
import { useI18n } from '@/system/i18n/use-i18n'
import { useContestRanklistPageModel } from './hooks/useContestRanklistPageModel'

const ranklistItemsPerPage = 10

export function ContestRanklistPage() {
  const { t } = useI18n()
  usePageTitle(t('contest.ranklist.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const { slug } = useParams<{ slug: string }>()
  const [searchParams, setSearchParams] = useSearchParams()
  const currentPage = parsePositivePage(searchParams.get('page'))
  const slugResult = parseContestSlug(slug ?? '')

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  if (!slugResult.ok) {
    return <Navigate replace to="/contests" />
  }

  return (
    <ContestRanklistPageContent
      contestSlug={slugResult.value}
      currentPage={currentPage}
      searchParams={searchParams}
      setSearchParams={setSearchParams}
    />
  )
}

function ContestRanklistPageContent({
  contestSlug,
  currentPage,
  searchParams,
  setSearchParams,
}: {
  contestSlug: ContestSlug
  currentPage: number
  searchParams: URLSearchParams
  setSearchParams: ReturnType<typeof useSearchParams>[1]
}) {
  const { t } = useI18n()
  const model = useContestRanklistPageModel(contestSlug, { page: currentPage, pageSize: ranklistItemsPerPage })
  const totalPages = calculateTotalPages(model.totalItems, model.pageSize)
  const pageNumbers = buildPageNumbers(currentPage, totalPages)

  usePageSearchParamCorrection({
    currentPage,
    totalPages,
    isLoading: model.isLoading,
    setSearchParams,
  })

  const onPageChange = (page: number) => {
    const nextSearchParams = new URLSearchParams(searchParams)
    nextSearchParams.set('page', String(page))
    setSearchParams(nextSearchParams)
  }

  return (
    <PageShell title={t('contest.ranklist.heading')} mainClassName="bg-[linear-gradient(180deg,#f0fdfa_0%,#ecfeff_48%,#f8fafc_100%)]">
      <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
              <Trophy className="size-5" />
            </div>
            <CardTitle className="text-xl text-slate-950">{t('contest.ranklist.cardTitle')}</CardTitle>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {model.errorMessage ? (
            <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
              <AlertDescription className="text-rose-700">{model.errorMessage}</AlertDescription>
            </Alert>
          ) : null}
          {model.isLoading ? (
            <p className="text-sm text-slate-500">{t('contest.ranklist.loading')}</p>
          ) : model.items.length === 0 ? (
            <p className="text-sm text-slate-500">{t('contest.ranklist.empty')}</p>
          ) : (
            <ContestRanklistTable items={model.items} />
          )}
          {!model.isLoading && model.items.length > 0 && totalPages > 1 ? (
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
    </PageShell>
  )
}

function ContestRanklistTable({ items }: { items: ContestRanklistItem[] }) {
  const { t } = useI18n()
  const problemColumns = items[0]?.problemResults.map((result) => result.problem) ?? []
  const maxTotalScore = Math.max(problemColumns.length * 100, 1)

  return (
    <div className="overflow-x-auto rounded-2xl border border-slate-200">
      <Table>
        <TableHeader>
          <TableRow className="bg-slate-50/80">
            <TableHead className="whitespace-nowrap">{t('contest.ranklist.rank')}</TableHead>
            <TableHead className="whitespace-nowrap">{t('contest.ranklist.user')}</TableHead>
            <TableHead className="whitespace-nowrap">{t('contest.ranklist.score')}</TableHead>
            <TableHead className="whitespace-nowrap">{t('contest.ranklist.penalty')}</TableHead>
            {problemColumns.map((problem) => (
              <TableHead key={problem.id} className="min-w-28 text-center" title={problemTitleValue(problem.title)}>
                {contestProblemAliasValue(problem.alias)}
              </TableHead>
            ))}
          </TableRow>
        </TableHeader>
        <TableBody>
          {items.map((item) => (
            <TableRow key={item.user.username}>
              <TableCell className="font-mono text-slate-600">#{contestRankValue(item.rank)}</TableCell>
              <TableCell className="min-w-40">
                <UserProfileLink className="inline-flex items-center gap-2" showUsername user={item.user} />
              </TableCell>
              <TableCell className="font-semibold" style={scoreColorStyle(contestScoreValue(item.totalScore), maxTotalScore)}>
                {formatContestScore(item.totalScore)}
              </TableCell>
              <TableCell className="font-mono text-slate-600">{formatPenaltyMillis(item.penaltyMillis)}</TableCell>
              {item.problemResults.map((result) => (
                <TableCell key={result.problem.id} className="min-w-28 text-center">
                  <ContestProblemResultCell result={result} />
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}

function ContestProblemResultCell({ result }: { result: ContestRanklistProblemResult }) {
  if (result.score === null || result.submissionId === null) {
    return <span className="text-slate-300">-</span>
  }

  const score = contestScoreValue(result.score)
  return (
    <div className="flex flex-col items-center gap-1">
      <Link
        className="rounded-full px-3 py-1 text-sm font-bold transition hover:scale-[1.03] hover:shadow-sm"
        style={scorePillStyle(score)}
        to={`/submissions/${submissionIdValue(result.submissionId)}`}
      >
        {formatContestScore(result.score)}
      </Link>
      {result.penaltyMillis ? <span className="font-mono text-[11px] text-slate-500">{formatPenaltyMillis(result.penaltyMillis)}</span> : null}
    </div>
  )
}

function formatContestScore(score: ContestRanklistItem['totalScore']): string {
  return contestScoreValue(score).toLocaleString(undefined, { maximumFractionDigits: 2 })
}

function formatPenaltyMillis(penaltyMillis: ContestRanklistItem['penaltyMillis']): string {
  const totalSeconds = Math.floor(contestPenaltyMillisValue(penaltyMillis) / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  return [hours, minutes, seconds].map((value) => String(value).padStart(2, '0')).join(':')
}

function scoreColorStyle(score: number, maxScore: number): CSSProperties {
  const ratio = clampScoreRatio(score / maxScore)
  return {
    color: scoreHueColor(ratio),
  }
}

function scorePillStyle(score: number): CSSProperties {
  const ratio = clampScoreRatio(score / 100)
  return {
    backgroundColor: `hsla(${scoreHue(ratio)}, 82%, 92%, 0.9)`,
    color: scoreHueColor(ratio),
  }
}

function scoreHueColor(ratio: number): string {
  return `hsl(${scoreHue(ratio)}, 72%, 34%)`
}

function scoreHue(ratio: number): number {
  return Math.round(142 * ratio)
}

function clampScoreRatio(ratio: number): number {
  if (!Number.isFinite(ratio)) {
    return 0
  }
  return Math.min(1, Math.max(0, ratio))
}
