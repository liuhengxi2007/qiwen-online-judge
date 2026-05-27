import { Button } from '@/components/ui/button'
import type { BlogSummary } from '@/objects/blog/response/BlogSummary'
import { BlogSummaryCard } from './blog-summary-card'
import type { BlogId } from '@/objects/blog/BlogId'
import { useI18n } from '@/system/i18n/use-i18n'

type PendingProblemBlogsCardProps = {
  pendingBlogs: BlogSummary[]
  isLoadingPending: boolean
  pendingMessage: string
  activeReviewBlogId: BlogId | null
  onAccept: (blog: BlogSummary) => void
  onReject: (blog: BlogSummary) => void
}

export function PendingProblemBlogsCard({
  pendingBlogs,
  isLoadingPending,
  pendingMessage,
  activeReviewBlogId,
  onAccept,
  onReject,
}: PendingProblemBlogsCardProps) {
  const { t } = useI18n()

  return (
    <div className="rounded-3xl border border-amber-100 bg-amber-50 p-4">
      <div className="flex flex-col gap-1">
        <h2 className="text-base font-semibold text-amber-950">{t('blog.problem.pendingTitle')}</h2>
        <p className="text-sm text-amber-800">{t('blog.problem.pendingDescription')}</p>
      </div>
      {pendingMessage ? <p className="mt-2 text-sm text-amber-800">{pendingMessage}</p> : null}
      {isLoadingPending ? (
        <p className="mt-3 text-sm text-amber-800">{t('blog.list.loading')}</p>
      ) : pendingBlogs.length === 0 ? (
        <p className="mt-3 text-sm text-amber-800">{t('blog.problem.pendingEmpty')}</p>
      ) : (
        <div className="mt-4 space-y-3">
          {pendingBlogs.map((blog) => (
            <BlogSummaryCard
              key={blog.id}
              blog={blog}
              className="rounded-2xl border border-amber-100 bg-white p-4"
              showVisibility={false}
              showRelatedProblems={false}
              showScore={false}
              showDate={false}
              actions={
                <div className="flex flex-wrap gap-2">
                  <Button
                    type="button"
                    disabled={activeReviewBlogId === blog.id}
                    className="rounded-2xl bg-emerald-300 text-emerald-950 hover:bg-emerald-400"
                    onClick={() => onAccept(blog)}
                  >
                    {t('blog.problem.accept')}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    disabled={activeReviewBlogId === blog.id}
                    className="rounded-2xl border-rose-200 bg-white text-rose-700"
                    onClick={() => onReject(blog)}
                  >
                    {t('blog.problem.reject')}
                  </Button>
                </div>
              }
            />
          ))}
        </div>
      )}
    </div>
  )
}
