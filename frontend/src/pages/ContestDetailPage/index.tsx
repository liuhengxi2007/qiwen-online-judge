import { Link, Navigate, useParams } from 'react-router-dom'
import { Rows3 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { contestDescriptionValue } from '@/objects/contest/ContestDescription'
import { contestProblemAliasValue } from '@/objects/contest/ContestProblemAlias'
import { contestSlugValue, parseContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestTitleValue } from '@/objects/contest/ContestTitle'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'
import { DateTimeText } from '@/pages/components/DateTimeText'
import { MarkdownDocument } from '@/pages/components/MarkdownDocument'
import { PageLoadingCard } from '@/pages/components/PageLoadingCard'
import { PageShell } from '@/pages/components/PageShell'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useProblemTitleDisplay } from '@/pages/hooks/useProblemTitleDisplay'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { resourceAccessBadgeLabel } from '@/pages/objects/ResourceAccessDisplay'
import { useI18n } from '@/system/i18n/use-i18n'
import { useContestDetailPageModel } from './hooks/useContestDetailPageModel'

/**
 * 比赛详情页入口，校验比赛 slug 后渲染详情内容。
 */
export function ContestDetailPage() {
  const { t } = useI18n()
  usePageTitle(t('contest.detail.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const { slug } = useParams<{ slug: string }>()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const slugResult = parseContestSlug(slug ?? '')
  if (!slugResult.ok) {
    return <Navigate replace to="/contests" />
  }

  return <ContestDetailPageContent contestSlug={slugResult.value} />
}

/**
 * 比赛详情页主体，负责会话守卫、详情查询、报名/取消报名和入口卡片展示。
 */
function ContestDetailPageContent({
  contestSlug,
}: {
  contestSlug: ContestSlug
}) {
  const { t } = useI18n()
  const model = useContestDetailPageModel(contestSlug)

  return (
    <PageShell title={t('contest.detail.heading')} mainClassName="bg-[linear-gradient(180deg,#f0fdfa_0%,#ecfeff_48%,#f8fafc_100%)]">
      {!model.isLoading && !model.contest && model.loadErrorMessage ? (
        <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{model.loadErrorMessage}</AlertDescription>
        </Alert>
      ) : null}

      {model.isLoading ? (
        <PageLoadingCard message={t('contest.detail.loading')} />
      ) : model.contest ? (
        <div className="space-y-6">
          <ContestDetailHeaderCard
            contest={model.contest}
          />
          <ContestProblemsCard contest={model.contest} />
        </div>
      ) : null}
    </PageShell>
  )
}

/**
 * 比赛详情头部卡片，展示比赛元信息和报名操作。
 */
function ContestDetailHeaderCard({
  contest,
}: {
  contest: ContestDetail
}) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <CardTitle className="text-2xl text-slate-950">{contestTitleValue(contest.title)}</CardTitle>
            <CardDescription className="mt-2 font-mono text-sm text-slate-500">
              {contestSlugValue(contest.slug)}
            </CardDescription>
          </div>
          <div className="flex flex-wrap gap-3">
            <Button asChild type="button" variant="outline" className="rounded-2xl border-cyan-200 bg-white text-cyan-800 hover:bg-cyan-50">
              <Link to={`/contests/${contestSlugValue(contest.slug)}/registrants`}>{t('contest.detail.registrants')}</Link>
            </Button>
            <Button asChild type="button" variant="outline" className="rounded-2xl border-cyan-200 bg-white text-cyan-800 hover:bg-cyan-50">
              <Link to={`/contests/${contestSlugValue(contest.slug)}/ranklist`}>{t('contest.detail.ranklist')}</Link>
            </Button>
            <Button asChild type="button" variant="outline" className="rounded-2xl border-cyan-200 bg-white text-cyan-800 hover:bg-cyan-50">
              <Link to={`/contests/${contestSlugValue(contest.slug)}/submissions`}>{t('contest.detail.submissions')}</Link>
            </Button>
            {contest.canManage ? (
              <>
                <Button asChild type="button" variant="outline" className="rounded-2xl border-slate-200 bg-white text-slate-800 hover:bg-slate-50">
                  <Link to={`/contests/${contestSlugValue(contest.slug)}/manage`}>{t('contest.detail.manage')}</Link>
                </Button>
                <Badge variant="outline" className="rounded-2xl px-4 py-2">
                  {t('contest.detail.managerView')}
                </Badge>
              </>
            ) : null}
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-wrap items-center gap-3">
          <Badge variant="secondary">{resourceAccessBadgeLabel(contest.accessPolicy, t)}</Badge>
        </div>
        <div className="grid gap-3 text-sm text-slate-600 sm:grid-cols-2">
          <p>
            <span className="font-medium text-slate-900">{t('contest.detail.startAt')} </span>
            <DateTimeText value={contest.startAt} />
          </p>
          <p>
            <span className="font-medium text-slate-900">{t('contest.detail.endAt')} </span>
            <DateTimeText value={contest.endAt} />
          </p>
        </div>
        <div className="rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
          {contestDescriptionValue(contest.description) ? (
            <MarkdownDocument content={contestDescriptionValue(contest.description)} />
          ) : (
            <p className="text-sm text-slate-500">{t('common.noDescription')}</p>
          )}
        </div>
        {contest.author ? (
          <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
            <span>{t('common.authorLabel')} </span>
            <UserProfileLink className="inline-flex items-baseline gap-2 normal-case tracking-normal" showUsername user={contest.author} />
          </p>
        ) : null}
      </CardContent>
    </Card>
  )
}

/**
 * 比赛题目卡片，展示比赛内题目列表和跳转入口。
 */
function ContestProblemsCard({ contest }: { contest: ContestDetail }) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
            <Rows3 className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl text-slate-950">{t('contest.detail.problemsTitle')}</CardTitle>
            <CardDescription>{t('contest.detail.problemsDescription')}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {contest.problems.length === 0 ? (
          <p className="text-sm text-slate-500">{t('contest.detail.emptyProblems')}</p>
        ) : (
          contest.problems.map((problem) => <ContestProblemItem key={problem.id} contestSlug={contest.slug} problem={problem} />)
        )}
      </CardContent>
    </Card>
  )
}

/**
 * 比赛题目条目，按比赛路径链接到对应题目详情。
 */
function ContestProblemItem({ contestSlug, problem }: { contestSlug: ContestSlug; problem: ContestDetail['problems'][number] }) {
  const { t } = useI18n()
  const titleText = useProblemTitleDisplay(problem.title, problem.slug)

  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="outline">{contestProblemAliasValue(problem.alias)}</Badge>
          <Link className="text-sm font-medium text-slate-900 hover:underline" to={`/contests/${contestSlugValue(contestSlug)}/problems/${problemSlugValue(problem.slug)}`}>
            {titleText}
          </Link>
        </div>
        <Button asChild type="button" variant="outline" className="rounded-2xl border-cyan-200 bg-white text-cyan-800 hover:bg-cyan-50">
          <Link to={`/contests/${contestSlugValue(contestSlug)}/problems/${problemSlugValue(problem.slug)}/submit`}>
            {t('problem.detail.submitCode')}
          </Link>
        </Button>
      </div>
    </div>
  )
}
