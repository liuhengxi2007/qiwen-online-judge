import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

import { blogIdValue, blogTitleValue } from '@/features/blog/lib/blog-parsers'
import type { BlogSummary } from '@/features/blog/http/response/BlogSummary'
import { blogScoreClassName } from '@/features/blog/components/blog-support'
import { formatProblemTitleDisplay } from '@/features/problem/lib/problem-display'
import { problemSlugValue } from '@/features/problem/lib/problem-parsers'
import { useProblemTitleDisplayMode } from '@/features/problem/hooks/use-problem-title-display'
import { DateTimeText } from '@/shared/components/date-time-text'
import { UserProfileLink } from '@/features/user/components/user-profile-link'
import { useI18n } from '@/shared/i18n/use-i18n'

type BlogSummaryCardProps = {
  blog: BlogSummary
  actions?: ReactNode
  className?: string
  showVisibility?: boolean
  showRelatedProblems?: boolean
  showScore?: boolean
  showDate?: boolean
}

export function BlogSummaryCard({
  blog,
  actions,
  className = 'rounded-3xl border border-slate-200 bg-slate-50 p-5',
  showVisibility = true,
  showRelatedProblems = true,
  showScore = true,
  showDate = true,
}: BlogSummaryCardProps) {
  const { t } = useI18n()
  const problemTitleDisplayMode = useProblemTitleDisplayMode()

  return (
    <article className={className}>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex gap-4">
          <div>
            <Link className="text-xl font-semibold text-slate-950 hover:underline" to={`/blogs/${blogIdValue(blog.id)}`}>
              {blogTitleValue(blog.title)}
            </Link>
            {showVisibility ? (
              <span className="ml-3 rounded-full bg-white px-3 py-1 text-xs font-semibold text-slate-600">
                {t(`blog.visibility.${blog.visibility}`)}
              </span>
            ) : null}
            {showRelatedProblems && blog.relatedProblems.length > 0 ? (
              <div className="mt-2 flex flex-wrap gap-2 text-sm text-slate-600">
                <span>{t('blog.problem.linkedTo')}</span>
                {blog.relatedProblems.slice(0, 3).map((problem) => (
                  <Link
                    key={problemSlugValue(problem.slug)}
                    className="font-semibold text-orange-700 hover:underline"
                    to={`/problems/${problemSlugValue(problem.slug)}`}
                  >
                    {formatProblemTitleDisplay(problem.title, problem.slug, problemTitleDisplayMode)}
                  </Link>
                ))}
              </div>
            ) : null}
            <p className="mt-2 text-xs uppercase tracking-[0.18em] text-slate-400">
              <span>{t('common.createdByLabel')} </span>
              <UserProfileLink className="inline-flex items-baseline gap-2 normal-case tracking-normal" user={blog.author} />
            </p>
          </div>
        </div>
        <div className="flex flex-col gap-2 sm:items-end">
          {showScore ? (
            <p className={`text-sm font-semibold ${blogScoreClassName(blog.score)}`}>
              {t('blog.vote.score', { score: String(blog.score) })}
            </p>
          ) : null}
          {showDate ? <DateTimeText className="text-sm text-slate-500" value={blog.createdAt} /> : null}
          {actions}
        </div>
      </div>
    </article>
  )
}
