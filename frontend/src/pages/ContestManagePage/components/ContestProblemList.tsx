import { Link } from 'react-router-dom'
import { Database, PencilLine } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { contestProblemAliasValue } from '@/objects/contest/ContestProblemAlias'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { problemTitleValue } from '@/objects/problem/ProblemTitle'
import { useI18n } from '@/system/i18n/use-i18n'

import type { ContestManagePageModel } from '../hooks/useContestManagePageModel'

type ContestProblemListProps = {
  contest: ContestDetail
  model: ContestManagePageModel
}

/**
 * 保留题目列表 props 解构：列表展示来自 contest，移除状态和动作来自 model。
 */
export function ContestProblemList({ contest, model }: ContestProblemListProps) {
  const { t } = useI18n()

  if (contest.problems.length === 0) {
    return (
      <div className="space-y-3">
        <p className="text-sm text-slate-500">{t('contest.detail.emptyProblems')}</p>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {contest.problems.map((problem) => (
        <div key={problem.id} className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-slate-50 p-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant="outline">{contestProblemAliasValue(problem.alias)}</Badge>
            <span className="text-sm font-medium text-slate-900">{problemTitleValue(problem.title)}</span>
            <span className="text-sm text-slate-500">{problemSlugValue(problem.slug)}</span>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button asChild type="button" variant="outline" className="rounded-2xl border-emerald-200 bg-white text-emerald-800 hover:bg-emerald-50">
              <Link to={`/contests/${contestSlugValue(contest.slug)}/problems/${problemSlugValue(problem.slug)}`}>
                <PencilLine className="size-4" />
                {t('problem.detail.edit')}
              </Link>
            </Button>
            <Button asChild type="button" variant="outline" className="rounded-2xl border-slate-200 bg-white text-slate-800 hover:bg-slate-50">
              <Link to={`/contests/${contestSlugValue(contest.slug)}/problems/${problemSlugValue(problem.slug)}/data`}>
                <Database className="size-4" />
                {t('problem.detail.manageData')}
              </Link>
            </Button>
            <Button
              type="button"
              variant="destructiveOutline"
              disabled={model.removingProblemSlug === problemSlugValue(problem.slug)}
              onClick={() => {
                void model.removeProblem(problemSlugValue(problem.slug))
              }}
            >
              {model.removingProblemSlug === problemSlugValue(problem.slug) ? t('contest.manage.removingProblem') : t('contest.manage.removeProblem')}
            </Button>
          </div>
        </div>
      ))}
    </div>
  )
}
