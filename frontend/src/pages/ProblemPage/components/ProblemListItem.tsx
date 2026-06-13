import { Link } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { resourceAccessBadgeLabel } from '@/pages/objects/ResourceAccessDisplay'
import { useI18n } from '@/system/i18n/use-i18n'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { useProblemTitleDisplay } from '@/pages/hooks/useProblemTitleDisplay'
import type { ProblemSummary } from '@/objects/problem/response/ProblemSummary'

/**
 * 题目列表项属性，包含题目摘要和是否补充显示 slug。
 */
type ProblemListItemProps = {
  problem: ProblemSummary
  showSlugSupplement: boolean
}

/**
 * 题目列表项组件，展示标题、slug、公开状态和创建时间。
 */
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
        <span>{t('common.authorLabel')} </span>
        {problem.author ? (
          <UserProfileLink className="inline-flex items-baseline gap-2 normal-case tracking-normal" showUsername user={problem.author} />
        ) : (
          <span className="normal-case tracking-normal">{t('common.noAuthor')}</span>
        )}
      </p>
    </div>
  )
}
