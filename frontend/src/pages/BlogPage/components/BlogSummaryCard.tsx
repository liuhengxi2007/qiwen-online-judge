import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

import { blogIdValue } from '@/objects/blog/BlogId'
import { blogTitleValue } from '@/objects/blog/BlogTitle'
import type { BlogSummary } from '@/objects/blog/response/BlogSummary'
import { blogScoreClassName } from '@/pages/objects/BlogDisplay'
import { formatProblemTitleDisplay } from '@/pages/objects/ProblemTitleDisplay'
import { resourceAccessBadgeLabel } from '@/pages/objects/ResourceAccessDisplay'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { useProblemTitleDisplayMode } from '@/pages/hooks/useProblemTitleDisplay'
import { DateTimeText } from '@/pages/components/DateTimeText'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 博客摘要卡片属性，包含博客摘要、当前用户名和显示偏好。
 */
type BlogSummaryCardProps = {
  blog: BlogSummary
  actions?: ReactNode
  className?: string
  showVisibility?: boolean
  showRelatedProblems?: boolean
  showScore?: boolean
  showDate?: boolean
}

/**
 * 博客摘要卡片，展示标题、作者、关联题目、投票得分和创建时间。
 */
export function BlogSummaryCard({
  blog,
  actions,
  className = 'rounded-3xl border border-slate-200 bg-slate-50 p-5',
  showVisibility = true,
  showRelatedProblems = true,
  showScore = true,
  showDate = true,
}: BlogSummaryCardProps) {
  // 保留扁平 props：核心数据和可选展示开关是卡片公开 API，调用端用具名开关比配置对象更易读。
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
                {resourceAccessBadgeLabel(blog.visibilityPolicy, t)}
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
              <span>{t('common.authorLabel')} </span>
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
