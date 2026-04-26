import { useNavigate, useParams, Navigate, Link } from 'react-router-dom'
import { NotebookPen, ThumbsDown, ThumbsUp } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { usernameValue } from '@/features/auth/domain/auth'
import { blogContentValue, blogTitleValue, parseBlogId, type BlogVisibility } from '@/features/blog/domain/blog'
import { BlogCommentThread } from '@/features/blog/components/blog-comment-thread'
import { blogScoreClassName, formatBlogDate } from '@/features/blog/components/blog-support'
import { useBlogDetailQuery } from '@/features/blog/hooks/use-blog-detail-query'
import { useBlogDetailPageModel } from '@/features/blog/hooks/use-blog-detail-page-model'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { formatProblemTitleDisplay, problemSlugValue, useProblemTitleDisplayMode } from '@/features/problem/domain/problem'
import { AppSectionBar } from '@/shared/components/app-section-bar'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { MarkdownDocument } from '@/shared/components/markdown-document'
import { UserProfileLink } from '@/shared/components/user-profile-link'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/i18n'

export function BlogDetailPage() {
  const { t } = useI18n()
  usePageTitle(t('blog.detail.pageTitle'))
  const problemTitleDisplayMode = useProblemTitleDisplayMode()
  const navigate = useNavigate()
  const { blogId: rawBlogId } = useParams()
  const parsedBlogId = rawBlogId ? parseBlogId(Number(rawBlogId)) : { ok: false as const, error: 'Blog id is required.' }
  const { session: user, navigationIntent } = useSessionGuard()
  const query = useBlogDetailQuery(parsedBlogId.ok ? parsedBlogId.value : null)
  const model = useBlogDetailPageModel({
    blog: query.blog,
    setBlog: query.setBlog,
    onDeleted: () => navigate('/blogs'),
  })

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#fff7ed_0%,#eef6ff_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('blog.detail.heading')}</h1>
          </div>
          <AncestorNavigation />
        </div>

        <AppSectionBar />

        {query.errorMessage ? (
          <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">
              {query.errorMessage === 'invalid' ? t('blog.detail.invalidUrl') : t('blog.detail.loadFailed')}
            </AlertDescription>
          </Alert>
        ) : null}

        <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-orange-100 text-orange-700">
                <NotebookPen className="size-5" />
              </div>
              <div>
                <CardTitle className="text-2xl text-slate-950">
                  {query.blog ? blogTitleValue(query.blog.title) : t('blog.detail.cardTitle')}
                </CardTitle>
                <CardDescription>
                  {t('blog.detail.cardDescription')}
                  {query.blog ? (
                    <span className="ml-3 rounded-full bg-orange-50 px-3 py-1 text-xs font-semibold text-orange-800">
                      {t(`blog.visibility.${query.blog.visibility}`)}
                    </span>
                  ) : null}
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            {query.isLoading ? (
              <p className="text-sm text-slate-500">{t('blog.detail.loading')}</p>
            ) : query.blog ? (
              <article>
                {usernameValue(query.blog.author.username) === usernameValue(user.username) ? (
                  <div className="mb-5 space-y-4">
                    <div className="flex flex-wrap gap-2">
                      <Button type="button" variant="outline" className="rounded-2xl border-slate-300 bg-white" onClick={model.startEditingBlog}>
                        {t('common.edit')}
                      </Button>
                      <Button type="button" variant="outline" className="rounded-2xl border-rose-200 bg-white text-rose-700" onClick={() => void model.removeBlog()}>
                        {t('common.delete')}
                      </Button>
                    </div>
                    {query.blog.visibility === 'public' ? (
                      <div className="rounded-3xl border border-orange-100 bg-orange-50 p-4">
                        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
                          <div className="space-y-2 sm:max-w-xs">
                            <label className="text-sm font-medium text-orange-950" htmlFor="submit-blog-problem-slug">
                              {t('blog.problem.submitToProblem')}
                            </label>
                            <Input
                              id="submit-blog-problem-slug"
                              value={model.submitProblemSlug}
                              onChange={(event) => model.setSubmitProblemSlug(event.target.value)}
                            />
                          </div>
                          <Button
                            type="button"
                            disabled={model.isSubmittingToProblem}
                            className="rounded-2xl bg-orange-300 text-orange-950 hover:bg-orange-400"
                            onClick={() => void model.submitToProblem()}
                          >
                            {model.isSubmittingToProblem ? t('common.loading') : t('blog.problem.submit')}
                          </Button>
                        </div>
                        {model.submitProblemMessage ? <p className="mt-2 text-sm text-orange-800">{model.submitProblemMessage}</p> : null}
                      </div>
                    ) : null}
                  </div>
                ) : null}

                {model.isEditingBlog ? (
                  <div className="mb-6 space-y-4 rounded-3xl border border-slate-200 bg-slate-50 p-5">
                    <Input value={model.editBlogTitle} onChange={(event) => model.setEditBlogTitle(event.target.value)} />
                    <Select value={model.editBlogVisibility} onValueChange={(value) => model.setEditBlogVisibility(value as BlogVisibility)}>
                      <SelectTrigger className="rounded-2xl border-slate-300 bg-white">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="public">{t('blog.visibility.public')}</SelectItem>
                        <SelectItem value="private">{t('blog.visibility.private')}</SelectItem>
                      </SelectContent>
                    </Select>
                    <Textarea className="min-h-56" value={model.editBlogContent} onChange={(event) => model.setEditBlogContent(event.target.value)} />
                    <div className="flex flex-wrap gap-2">
                      <Button type="button" className="rounded-2xl bg-slate-950 text-white" onClick={() => void model.submitBlogEdit()}>
                        {t('common.save')}
                      </Button>
                      <Button type="button" variant="outline" className="rounded-2xl border-slate-300 bg-white" onClick={() => model.setIsEditingBlog(false)}>
                        {t('common.cancel')}
                      </Button>
                    </div>
                  </div>
                ) : null}

                <div className="flex flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
                      <span>{t('common.createdByLabel')} </span>
                      <UserProfileLink className="inline-flex items-baseline gap-2 normal-case tracking-normal" user={query.blog.author} />
                    </p>
                    {query.blog.relatedProblems.length > 0 ? (
                      <div className="mt-2 flex flex-wrap gap-2 text-sm text-slate-600">
                        <span>{t('blog.problem.linkedTo')}</span>
                        {query.blog.relatedProblems.map((problem) => (
                          <Link key={problemSlugValue(problem.slug)} className="font-semibold text-orange-700 hover:underline" to={`/problems/${problemSlugValue(problem.slug)}`}>
                            {formatProblemTitleDisplay(problem.title, problem.slug, problemTitleDisplayMode)}
                          </Link>
                        ))}
                      </div>
                    ) : null}
                  </div>
                  <div className="flex flex-col gap-3 sm:items-end">
                    <p className="text-sm text-slate-500">{formatBlogDate(query.blog.createdAt)}</p>
                    <div className="flex flex-wrap items-center gap-2">
                      <span className={`text-sm font-semibold ${blogScoreClassName(query.blog.score)}`}>
                        {t('blog.vote.score', { score: String(query.blog.score) })}
                      </span>
                      <Button
                        type="button"
                        variant={query.blog.viewerVote === 'up' ? 'default' : 'outline'}
                        className={query.blog.viewerVote === 'up' ? 'rounded-2xl bg-emerald-600 text-white hover:bg-emerald-700' : 'rounded-2xl border-emerald-200 bg-white text-emerald-700'}
                        disabled={model.isVoting}
                        onClick={() => void model.submitVote('up')}
                      >
                        <ThumbsUp className="size-4" />
                        {t('blog.vote.up')}
                      </Button>
                      <Button
                        type="button"
                        variant={query.blog.viewerVote === 'down' ? 'default' : 'outline'}
                        className={query.blog.viewerVote === 'down' ? 'rounded-2xl bg-rose-600 text-white hover:bg-rose-700' : 'rounded-2xl border-rose-200 bg-white text-rose-700'}
                        disabled={model.isVoting}
                        onClick={() => void model.submitVote('down')}
                      >
                        <ThumbsDown className="size-4" />
                        {t('blog.vote.down')}
                      </Button>
                    </div>
                  </div>
                </div>

                <div className="mt-6 rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
                  <MarkdownDocument content={blogContentValue(query.blog.content)} />
                </div>

                <div className="mt-8 space-y-5">
                  <div>
                    <h2 className="text-lg font-semibold text-slate-950">{t('blog.comment.heading')}</h2>
                    <p className="mt-1 text-sm text-slate-500">{t('blog.comment.description')}</p>
                  </div>
                  <div className="space-y-3 rounded-3xl border border-slate-200 bg-white p-4">
                    <Textarea
                      value={model.commentContent}
                      className="min-h-28"
                      onChange={(event) => model.setCommentContent(event.target.value)}
                    />
                    {model.commentErrorMessage ? (
                      <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                        <AlertDescription className="text-rose-700">{model.commentErrorMessage}</AlertDescription>
                      </Alert>
                    ) : null}
                    <Button
                      type="button"
                      disabled={model.isSubmittingComment}
                      className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
                      onClick={() => void model.submitComment()}
                    >
                      {model.isSubmittingComment ? t('blog.comment.submitting') : t('blog.comment.submit')}
                    </Button>
                  </div>
                  <div className="space-y-3">
                    <h3 className="text-base font-semibold text-slate-950">{t('blog.comment.heading')}</h3>
                    <BlogCommentThread
                      comments={query.blog.comments}
                      currentUsername={usernameValue(user.username)}
                      votingCommentId={model.votingCommentId}
                      replyTargetId={model.replyTargetId}
                      replyContent={model.replyContent}
                      isSubmittingReply={model.isSubmittingReply}
                      editingCommentId={model.editingCommentId}
                      editingCommentContent={model.editingCommentContent}
                      commentErrorMessage={model.commentErrorMessage}
                      onReplyTargetChange={model.setReplyTargetId}
                      onReplyContentChange={model.setReplyContent}
                      onEditingCommentIdChange={model.setEditingCommentId}
                      onEditingCommentContentChange={model.setEditingCommentContent}
                      onSubmitReply={(commentId) => void model.submitReply(commentId)}
                      onSubmitCommentVote={(commentId, vote) => void model.submitCommentVote(commentId, vote)}
                      onStartEditingComment={model.startEditingComment}
                      onSubmitCommentEdit={(commentId) => void model.submitCommentEdit(commentId)}
                      onRemoveComment={(commentId) => void model.removeComment(commentId)}
                    />
                  </div>
                </div>
              </article>
            ) : null}
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
