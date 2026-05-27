import { Link } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { UserProfileLink } from '@/pages/components/user/user-profile-link'
import { resourceAccessBadgeLabel } from '@/objects/shared/resource-lifecycle'
import { useI18n } from '@/system/i18n/use-i18n'
import { problemSlugValue } from '@/objects/problem/problem-parsers'
import { useProblemTitleDisplay } from '@/pages/hooks/problem/use-problem-title-display'
import type { ProblemSummary } from '@/objects/problem/response/ProblemSummary'

type ProblemListItemProps = {
  problem: ProblemSummary
  showSlugSupplement: boolean
}

export function ProblemListItem({ problem, showSlugSupplement }: ProblemListItemProps) {
  const { t } = useI18n()
  const titleText = useProblemTitleDisplay(problem.title, problem.slug)

  return (
    <div className="rounded-3xl border border-slate-200 bg-slate-50 p-5">
      <div className="flex flex-wrap items-center gap-3">
        <Link className="text-lg font-semibold text-slate-950 hover:underline" to={`/problems/${problemSlugValue(problem.slug)}`}>
          {titleText}
        </Link>
        <Badge variant="secondary">{resourceAccessBadgeLabel(problem.accessPolicy)}</Badge>
      </div>
      {showSlugSupplement ? <p className="mt-2 font-mono text-sm text-slate-500">{problemSlugValue(problem.slug)}</p> : null}
      <p className="mt-4 text-xs uppercase tracking-[0.18em] text-slate-400">
        <span>{t('problem.createdByLabel')} </span>
        <UserProfileLink className="inline-flex items-baseline gap-2 normal-case tracking-normal" showUsername user={problem.creator} />
      </p>
    </div>
  )
}
