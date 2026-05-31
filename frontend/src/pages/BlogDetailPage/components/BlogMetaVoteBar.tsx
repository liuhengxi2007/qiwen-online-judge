import { Link } from 'react-router-dom'
import { ThumbsDown, ThumbsUp } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { blogScoreClassName } from '@/pages/objects/BlogDisplay'
import type { useBlogDetailPageModel } from '../hooks/useBlogDetailPageModel'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import { formatProblemTitleDisplay } from '@/pages/objects/ProblemTitleDisplay'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import { DateTimeText } from '@/pages/components/DateTimeText'
import { useI18n } from '@/system/i18n/use-i18n'

type BlogDetailPageModel = ReturnType<typeof useBlogDetailPageModel>

type BlogMetaVoteBarProps = {
  blog: BlogDetail
  model: BlogDetailPageModel
  problemTitleDisplayMode: ProblemTitleDisplayMode
}

export function BlogMetaVoteBar({ blog, model, problemTitleDisplayMode }: BlogMetaVoteBarProps) {
  const { t } = useI18n()

  return (
    <div className="flex flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
          <span>{t('common.authorLabel')} </span>
          <UserProfileLink className="inline-flex items-baseline gap-2 normal-case tracking-normal" user={blog.author} />
        </p>
        {blog.relatedProblems.length > 0 ? (
          <div className="mt-2 flex flex-wrap gap-2 text-sm text-slate-600">
            <span>{t('blog.problem.linkedTo')}</span>
            {blog.relatedProblems.map((problem) => (
              <Link key={problemSlugValue(problem.slug)} className="font-semibold text-orange-700 hover:underline" to={`/problems/${problemSlugValue(problem.slug)}`}>
                {formatProblemTitleDisplay(problem.title, problem.slug, problemTitleDisplayMode)}
              </Link>
            ))}
          </div>
        ) : null}
      </div>
      <div className="flex flex-col gap-3 sm:items-end">
        <DateTimeText className="text-sm text-slate-500" value={blog.createdAt} />
        <div className="flex flex-wrap items-center gap-2">
          <span className={`text-sm font-semibold ${blogScoreClassName(blog.score)}`}>
            {t('blog.vote.score', { score: String(blog.score) })}
          </span>
          <Button
            type="button"
            variant={blog.viewerVote === 'up' ? 'default' : 'outline'}
            className={blog.viewerVote === 'up' ? 'rounded-2xl bg-emerald-600 text-white hover:bg-emerald-700' : 'rounded-2xl border-emerald-200 bg-white text-emerald-700'}
            disabled={model.isVoting}
            onClick={() => void model.submitVote('up')}
          >
            <ThumbsUp className="size-4" />
            {t('blog.vote.up')}
          </Button>
          <Button
            type="button"
            variant={blog.viewerVote === 'down' ? 'default' : 'outline'}
            className={blog.viewerVote === 'down' ? 'rounded-2xl bg-rose-600 text-white hover:bg-rose-700' : 'rounded-2xl border-rose-200 bg-white text-rose-700'}
            disabled={model.isVoting}
            onClick={() => void model.submitVote('down')}
          >
            <ThumbsDown className="size-4" />
            {t('blog.vote.down')}
          </Button>
        </div>
      </div>
    </div>
  )
}
