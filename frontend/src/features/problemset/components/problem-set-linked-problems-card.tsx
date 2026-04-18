import { Link } from 'react-router-dom'
import { Rows3 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  problemSlugValue,
  shouldShowProblemSlugSupplement,
  useProblemTitleDisplay,
  useProblemTitleDisplayMode,
} from '@/features/problem/domain/problem'
import {
  type ProblemSetProblemSummary,
} from '@/features/problemset/domain/problemset'
import { useI18n } from '@/shared/i18n/i18n'

type ProblemSetLinkedProblemsCardProps = {
  problems: ProblemSetProblemSummary[]
  canManageProblems: boolean
  activeRemovingProblemSlug: string | null
  errorMessage: string
  successMessage: string
  onRemoveProblem: (problemSlug: ProblemSetProblemSummary['slug']) => void
}

export function ProblemSetLinkedProblemsCard({
  problems,
  canManageProblems,
  activeRemovingProblemSlug,
  errorMessage,
  successMessage,
  onRemoveProblem,
}: ProblemSetLinkedProblemsCardProps) {
  const { t } = useI18n()
  const problemTitleDisplayMode = useProblemTitleDisplayMode()
  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-700">
            <Rows3 className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl text-slate-950">{t('problemSet.detail.linkedProblemsTitle')}</CardTitle>
            <CardDescription>{t('problemSet.detail.linkedProblemsDescription')}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {errorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{errorMessage}</AlertDescription>
          </Alert>
        ) : null}
        {successMessage ? (
          <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
            <AlertDescription className="text-emerald-700">{successMessage}</AlertDescription>
          </Alert>
        ) : null}
        {problems.length === 0 ? (
          <p className="text-sm text-slate-500">{t('problemSet.detail.emptyProblems')}</p>
        ) : (
          problems.map((problem) => (
            <LinkedProblemItem
              key={problem.id}
              problem={problem}
              canManageProblems={canManageProblems}
              activeRemovingProblemSlug={activeRemovingProblemSlug}
              showSlugSupplement={shouldShowProblemSlugSupplement(problemTitleDisplayMode)}
              onRemoveProblem={onRemoveProblem}
            />
          ))
        )}
      </CardContent>
    </Card>
  )
}

function LinkedProblemItem({
  problem,
  canManageProblems,
  activeRemovingProblemSlug,
  showSlugSupplement,
  onRemoveProblem,
}: {
  problem: ProblemSetProblemSummary
  canManageProblems: boolean
  activeRemovingProblemSlug: string | null
  showSlugSupplement: boolean
  onRemoveProblem: (problemSlug: ProblemSetProblemSummary['slug']) => void
}) {
  const { t } = useI18n()
  const titleText = useProblemTitleDisplay(problem.title, problem.slug)

  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant="outline">#{problem.position}</Badge>
            <Link className="text-sm font-medium text-slate-900 hover:underline" to={`/problems/${problemSlugValue(problem.slug)}`}>
              {titleText}
            </Link>
          </div>
          {showSlugSupplement ? <p className="mt-1 font-mono text-xs text-slate-500">{problemSlugValue(problem.slug)}</p> : null}
        </div>
        {canManageProblems ? (
          <Button
            type="button"
            variant="outline"
            className="rounded-2xl border-rose-200 bg-white text-rose-700 hover:bg-rose-50 hover:text-rose-800"
            disabled={activeRemovingProblemSlug === problem.slug}
            onClick={() => {
              onRemoveProblem(problem.slug)
            }}
          >
            {activeRemovingProblemSlug === problem.slug ? t('problemSet.detail.removingProblem') : t('problemSet.detail.removeProblem')}
          </Button>
        ) : null}
      </div>
    </div>
  )
}
