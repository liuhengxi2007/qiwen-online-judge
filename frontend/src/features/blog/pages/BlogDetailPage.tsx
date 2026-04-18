import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { NotebookPen, ThumbsDown, ThumbsUp } from 'lucide-react'
import { useEffect, useState, type ReactNode } from 'react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { usernameValue, type Username } from '@/features/auth/domain/auth'
import { blogCommentContentValue, blogCommentIdValue, blogContentValue, blogTitleValue, parseBlogCommentContent, parseBlogContent, parseBlogId, parseBlogTitle, type BlogCommentId, type BlogCommentSummary, type BlogType, type BlogVisibility, type BlogVote } from '@/features/blog/domain/blog'
import { createBlogComment, deleteBlog, deleteBlogComment, updateBlog, updateBlogComment, voteBlog, voteBlogComment } from '@/features/blog/api/blog-client'
import { useBlogDetailQuery } from '@/features/blog/hooks/use-blog-detail-query'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { listProblems } from '@/features/problem/api/problem-client'
import {
  formatProblemTitleDisplay,
  parseProblemSlug,
  problemSlugValue,
  useProblemTitleDisplayMode,
  type ProblemSummary,
} from '@/features/problem/domain/problem'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { MarkdownDocument } from '@/shared/components/markdown-document'
import { SignedInUser } from '@/shared/components/signed-in-user'
import { UserProfileLink } from '@/shared/components/user-profile-link'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/i18n'

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function scoreClassName(score: number): string {
  if (score > 0) {
    return 'text-emerald-700'
  }

  if (score < 0) {
    return 'text-rose-700'
  }

  return 'text-slate-700'
}

export function BlogDetailPage() {
  const { t } = useI18n()
  usePageTitle(t('blog.detail.pageTitle'))
  const problemTitleDisplayMode = useProblemTitleDisplayMode()
  const navigate = useNavigate()
  const { blogId: rawBlogId } = useParams()
  const parsedBlogId = rawBlogId ? parseBlogId(Number(rawBlogId)) : { ok: false as const, error: 'Blog id is required.' }
  const { session: user, navigationIntent } = useSessionGuard()
  const model = useBlogDetailQuery(parsedBlogId.ok ? parsedBlogId.value : null)
  const [isVoting, setIsVoting] = useState(false)
  const [votingCommentId, setVotingCommentId] = useState<BlogCommentId | null>(null)
  const [commentContent, setCommentContent] = useState('')
  const [replyTargetId, setReplyTargetId] = useState<BlogCommentId | null>(null)
  const [replyContent, setReplyContent] = useState('')
  const [isSubmittingComment, setIsSubmittingComment] = useState(false)
  const [isSubmittingReply, setIsSubmittingReply] = useState(false)
  const [commentErrorMessage, setCommentErrorMessage] = useState('')
  const [isEditingBlog, setIsEditingBlog] = useState(false)
  const [editBlogTitle, setEditBlogTitle] = useState('')
  const [editBlogContent, setEditBlogContent] = useState('')
  const [editBlogVisibility, setEditBlogVisibility] = useState<BlogVisibility>('public')
  const [editBlogType, setEditBlogType] = useState<BlogType>('general')
  const [editProblemSlug, setEditProblemSlug] = useState('')
  const [problems, setProblems] = useState<ProblemSummary[]>([])
  const [editingCommentId, setEditingCommentId] = useState<BlogCommentId | null>(null)
  const [editingCommentContent, setEditingCommentContent] = useState('')

  useEffect(() => {
    let isActive = true
    listProblems()
      .then((response) => {
        if (isActive) {
          setProblems(response.items)
        }
      })
      .catch(() => {
        if (isActive) {
          setProblems([])
        }
      })

    return () => {
      isActive = false
    }
  }, [])

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const currentUsername = user.username

  async function submitVote(vote: BlogVote) {
    if (!model.blog) {
      return
    }

    setIsVoting(true)
    try {
      const updatedBlog = await voteBlog(model.blog.id, { vote })
      model.setBlog(updatedBlog)
    } finally {
      setIsVoting(false)
    }
  }

  function isOwnUsername(username: Username): boolean {
    return usernameValue(currentUsername) === usernameValue(username)
  }

  function startEditingBlog() {
    if (!model.blog) {
      return
    }
    setEditBlogTitle(blogTitleValue(model.blog.title))
    setEditBlogContent(blogContentValue(model.blog.content))
    setEditBlogVisibility(model.blog.visibility)
    setEditBlogType(model.blog.blogType)
    setEditProblemSlug(model.blog.problemSlug === null ? '' : problemSlugValue(model.blog.problemSlug))
    setIsEditingBlog(true)
  }

  async function submitBlogEdit() {
    if (!model.blog) {
      return
    }
    const parsedTitle = parseBlogTitle(editBlogTitle)
    const parsedContent = parseBlogContent(editBlogContent)
    if (!parsedTitle.ok) {
      setCommentErrorMessage(parsedTitle.error)
      return
    }
    if (!parsedContent.ok) {
      setCommentErrorMessage(parsedContent.error)
      return
    }
    const problemSlug =
      editBlogType === 'problem'
        ? parseProblemSlug(editProblemSlug)
        : { ok: true as const, value: null }
    if (!problemSlug.ok) {
      setCommentErrorMessage(problemSlug.error)
      return
    }
    const updatedBlog = await updateBlog(model.blog.id, {
      title: parsedTitle.value,
      content: parsedContent.value,
      visibility: editBlogVisibility,
      blogType: editBlogType,
      problemSlug: problemSlug.value,
    })
    model.setBlog(updatedBlog)
    setIsEditingBlog(false)
  }

  async function removeBlog() {
    if (!model.blog || !window.confirm(t('blog.delete.confirm'))) {
      return
    }
    await deleteBlog(model.blog.id)
    navigate('/blogs')
  }

  async function submitComment() {
    if (!model.blog) {
      return
    }

    const parsedContent = parseBlogCommentContent(commentContent)
    if (!parsedContent.ok) {
      setCommentErrorMessage(parsedContent.error)
      return
    }

    setIsSubmittingComment(true)
    setCommentErrorMessage('')
    try {
      const updatedBlog = await createBlogComment(model.blog.id, { content: parsedContent.value })
      model.setBlog(updatedBlog)
      setCommentContent('')
    } catch {
      setCommentErrorMessage(t('blog.comment.createFailed'))
    } finally {
      setIsSubmittingComment(false)
    }
  }

  async function submitReply(parentCommentId: BlogCommentId) {
    if (!model.blog) {
      return
    }

    const parsedContent = parseBlogCommentContent(replyContent)
    if (!parsedContent.ok) {
      setCommentErrorMessage(parsedContent.error)
      return
    }

    setIsSubmittingReply(true)
    setCommentErrorMessage('')
    try {
      const updatedBlog = await createBlogComment(model.blog.id, { content: parsedContent.value }, parentCommentId)
      model.setBlog(updatedBlog)
      setReplyTargetId(null)
      setReplyContent('')
    } catch {
      setCommentErrorMessage(t('blog.comment.replyFailed'))
    } finally {
      setIsSubmittingReply(false)
    }
  }

  async function submitCommentVote(commentId: BlogCommentId, vote: BlogVote) {
    if (!model.blog) {
      return
    }

    setVotingCommentId(commentId)
    try {
      const updatedBlog = await voteBlogComment(model.blog.id, commentId, { vote })
      model.setBlog(updatedBlog)
    } finally {
      setVotingCommentId(null)
    }
  }

  function startEditingComment(comment: BlogCommentSummary) {
    setEditingCommentId(comment.id)
    setEditingCommentContent(blogCommentContentValue(comment.content))
    setCommentErrorMessage('')
  }

  async function submitCommentEdit(commentId: BlogCommentId) {
    if (!model.blog) {
      return
    }
    const parsedContent = parseBlogCommentContent(editingCommentContent)
    if (!parsedContent.ok) {
      setCommentErrorMessage(parsedContent.error)
      return
    }
    const updatedBlog = await updateBlogComment(model.blog.id, commentId, { content: parsedContent.value })
    model.setBlog(updatedBlog)
    setEditingCommentId(null)
    setEditingCommentContent('')
  }

  async function removeComment(commentId: BlogCommentId) {
    if (!model.blog || !window.confirm(t('blog.comment.deleteConfirm'))) {
      return
    }
    const updatedBlog = await deleteBlogComment(model.blog.id, commentId)
    model.setBlog(updatedBlog)
  }

  function childComments(parentId: BlogCommentId): BlogCommentSummary[] {
    if (!model.blog) {
      return []
    }

    return model.blog.comments.filter((comment) => comment.parentId !== null && blogCommentIdValue(comment.parentId) === blogCommentIdValue(parentId))
  }

  function renderComment(comment: BlogCommentSummary, depth: number): ReactNode {
    const canManageComment = isOwnUsername(comment.author.username)
    return (
      <div key={comment.id} className={depth === 0 ? 'rounded-3xl border border-slate-200 bg-white p-4' : 'ml-6 rounded-3xl border border-slate-200 bg-slate-50 p-4'}>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <UserProfileLink className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500" user={comment.author} />
            {editingCommentId !== null && blogCommentIdValue(editingCommentId) === blogCommentIdValue(comment.id) ? (
              <div className="mt-3 space-y-2">
                <Textarea value={editingCommentContent} onChange={(event) => setEditingCommentContent(event.target.value)} />
                <div className="flex gap-2">
                  <Button size="sm" className="rounded-xl bg-slate-950 text-white" onClick={() => void submitCommentEdit(comment.id)}>{t('common.save')}</Button>
                  <Button size="sm" variant="outline" className="rounded-xl" onClick={() => setEditingCommentId(null)}>{t('common.cancel')}</Button>
                </div>
              </div>
            ) : (
              <p className="mt-2 whitespace-pre-wrap text-sm leading-7 text-slate-800">{blogCommentContentValue(comment.content)}</p>
            )}
          </div>
          <div className="flex flex-wrap items-center gap-2 sm:justify-end">
            <span className={`text-xs font-semibold ${scoreClassName(comment.score)}`}>
              {t('blog.vote.score', { score: String(comment.score) })}
            </span>
            <Button
              type="button"
              size="sm"
              variant={comment.viewerVote === 'up' ? 'default' : 'outline'}
              className={comment.viewerVote === 'up' ? 'h-8 rounded-xl bg-emerald-600 px-2 text-xs text-white hover:bg-emerald-700' : 'h-8 rounded-xl border-emerald-200 bg-white px-2 text-xs text-emerald-700'}
              disabled={votingCommentId === comment.id}
              onClick={() => {
                void submitCommentVote(comment.id, 'up')
              }}
            >
              <ThumbsUp className="size-3" />
              {t('blog.vote.up')}
            </Button>
            <Button
              type="button"
              size="sm"
              variant={comment.viewerVote === 'down' ? 'default' : 'outline'}
              className={comment.viewerVote === 'down' ? 'h-8 rounded-xl bg-rose-600 px-2 text-xs text-white hover:bg-rose-700' : 'h-8 rounded-xl border-rose-200 bg-white px-2 text-xs text-rose-700'}
              disabled={votingCommentId === comment.id}
              onClick={() => {
                void submitCommentVote(comment.id, 'down')
              }}
            >
              <ThumbsDown className="size-3" />
              {t('blog.vote.down')}
            </Button>
            <Button
              type="button"
              size="sm"
              variant="outline"
              className="h-8 rounded-xl border-slate-300 bg-white px-2 text-xs"
              onClick={() => {
                setReplyTargetId(comment.id)
                setReplyContent('')
                setCommentErrorMessage('')
              }}
            >
              {t('blog.comment.reply')}
            </Button>
            {canManageComment ? (
              <>
                <Button type="button" size="sm" variant="outline" className="h-8 rounded-xl border-slate-300 bg-white px-2 text-xs" onClick={() => startEditingComment(comment)}>
                  {t('common.edit')}
                </Button>
                <Button type="button" size="sm" variant="outline" className="h-8 rounded-xl border-rose-200 bg-white px-2 text-xs text-rose-700" onClick={() => void removeComment(comment.id)}>
                  {t('common.delete')}
                </Button>
              </>
            ) : null}
          </div>
        </div>
        {replyTargetId !== null && blogCommentIdValue(replyTargetId) === blogCommentIdValue(comment.id) ? (
          <div className="mt-4 space-y-3 rounded-2xl border border-slate-200 bg-white p-3">
            <Textarea
              value={replyContent}
              className="min-h-24"
              placeholder={t('blog.comment.replyPlaceholder')}
              onChange={(event) => setReplyContent(event.target.value)}
            />
            <div className="flex flex-wrap gap-2">
              <Button
                type="button"
                size="sm"
                disabled={isSubmittingReply}
                className="rounded-xl bg-slate-950 text-white hover:bg-slate-800"
                onClick={() => {
                  void submitReply(comment.id)
                }}
              >
                {isSubmittingReply ? t('blog.comment.submitting') : t('blog.comment.replySubmit')}
              </Button>
              <Button
                type="button"
                size="sm"
                variant="outline"
                className="rounded-xl border-slate-300 bg-white"
                onClick={() => {
                  setReplyTargetId(null)
                  setReplyContent('')
                }}
              >
                {t('common.cancel')}
              </Button>
            </div>
          </div>
        ) : null}
        {childComments(comment.id).length > 0 ? (
          <div className="mt-4 space-y-3">
            {childComments(comment.id).map((child) => renderComment(child, depth + 1))}
          </div>
        ) : null}
      </div>
    )
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#fff7ed_0%,#eef6ff_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-5xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('blog.detail.heading')}</h1>
            <SignedInUser user={user} />
          </div>

          <AncestorNavigation />
        </div>

        {model.errorMessage ? (
          <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">
              {model.errorMessage === 'invalid' ? t('blog.detail.invalidUrl') : t('blog.detail.loadFailed')}
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
                  {model.blog ? blogTitleValue(model.blog.title) : t('blog.detail.cardTitle')}
                </CardTitle>
                <CardDescription>
                  {t('blog.detail.cardDescription')}
                  {model.blog ? (
                    <>
                      <span className="ml-3 rounded-full bg-orange-50 px-3 py-1 text-xs font-semibold text-orange-800">
                        {t(`blog.visibility.${model.blog.visibility}`)}
                      </span>
                      <span className="ml-2 rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
                        {t(`blog.type.${model.blog.blogType}`)}
                      </span>
                    </>
                  ) : null}
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            {model.isLoading ? (
              <p className="text-sm text-slate-500">{t('blog.detail.loading')}</p>
            ) : model.blog ? (
              <article>
                {isOwnUsername(model.blog.author.username) ? (
                  <div className="mb-5 flex flex-wrap gap-2">
                    <Button type="button" variant="outline" className="rounded-2xl border-slate-300 bg-white" onClick={startEditingBlog}>
                      {t('common.edit')}
                    </Button>
                    <Button type="button" variant="outline" className="rounded-2xl border-rose-200 bg-white text-rose-700" onClick={() => void removeBlog()}>
                      {t('common.delete')}
                    </Button>
                  </div>
                ) : null}
                {isEditingBlog ? (
                  <div className="mb-6 space-y-4 rounded-3xl border border-slate-200 bg-slate-50 p-5">
                    <Input value={editBlogTitle} onChange={(event) => setEditBlogTitle(event.target.value)} />
                    <Select value={editBlogVisibility} onValueChange={(value) => setEditBlogVisibility(value as BlogVisibility)}>
                      <SelectTrigger className="rounded-2xl border-slate-300 bg-white">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="public">{t('blog.visibility.public')}</SelectItem>
                        <SelectItem value="private">{t('blog.visibility.private')}</SelectItem>
                      </SelectContent>
                    </Select>
                    <Select
                      value={editBlogType}
                      onValueChange={(value) => {
                        const nextType = value as BlogType
                        setEditBlogType(nextType)
                        if (nextType === 'general') {
                          setEditProblemSlug('')
                        }
                      }}
                    >
                      <SelectTrigger className="rounded-2xl border-slate-300 bg-white">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="general">{t('blog.type.general')}</SelectItem>
                        <SelectItem value="problem">{t('blog.type.problem')}</SelectItem>
                      </SelectContent>
                    </Select>
                    {editBlogType === 'problem' ? (
                      <Select value={editProblemSlug} onValueChange={setEditProblemSlug}>
                        <SelectTrigger className="rounded-2xl border-slate-300 bg-white">
                          <SelectValue placeholder={t('blog.create.problemPlaceholder')} />
                        </SelectTrigger>
                        <SelectContent>
                          {problems.map((problem) => (
                            <SelectItem key={problemSlugValue(problem.slug)} value={problemSlugValue(problem.slug)}>
                              {formatProblemTitleDisplay(problem.title, problem.slug, problemTitleDisplayMode)}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    ) : null}
                    <Textarea className="min-h-56" value={editBlogContent} onChange={(event) => setEditBlogContent(event.target.value)} />
                    <div className="flex flex-wrap gap-2">
                      <Button type="button" className="rounded-2xl bg-slate-950 text-white" onClick={() => void submitBlogEdit()}>
                        {t('common.save')}
                      </Button>
                      <Button type="button" variant="outline" className="rounded-2xl border-slate-300 bg-white" onClick={() => setIsEditingBlog(false)}>
                        {t('common.cancel')}
                      </Button>
                    </div>
                  </div>
                ) : null}
                <div className="flex flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
                      <span>{t('common.createdByLabel')} </span>
                      <UserProfileLink className="inline-flex items-baseline gap-2 normal-case tracking-normal" user={model.blog.author} />
                    </p>
                    {model.blog.problemSlug !== null && model.blog.problemTitle !== null ? (
                      <p className="mt-2 text-sm text-slate-600">
                        {t('blog.problem.linkedTo')}{' '}
                        <Link className="font-semibold text-orange-700 hover:underline" to={`/problems/${problemSlugValue(model.blog.problemSlug)}`}>
                          {formatProblemTitleDisplay(model.blog.problemTitle, model.blog.problemSlug, problemTitleDisplayMode)}
                        </Link>
                      </p>
                    ) : null}
                  </div>
                  <div className="flex flex-col gap-3 sm:items-end">
                    <p className="text-sm text-slate-500">{formatDate(model.blog.createdAt)}</p>
                    <div className="flex flex-wrap items-center gap-2">
                      <span className={`text-sm font-semibold ${scoreClassName(model.blog.score)}`}>
                        {t('blog.vote.score', { score: String(model.blog.score) })}
                      </span>
                      <Button
                        type="button"
                        variant={model.blog.viewerVote === 'up' ? 'default' : 'outline'}
                        className={model.blog.viewerVote === 'up' ? 'rounded-2xl bg-emerald-600 text-white hover:bg-emerald-700' : 'rounded-2xl border-emerald-200 bg-white text-emerald-700'}
                        disabled={isVoting}
                        onClick={() => {
                          void submitVote('up')
                        }}
                      >
                        <ThumbsUp className="size-4" />
                        {t('blog.vote.up')}
                      </Button>
                      <Button
                        type="button"
                        variant={model.blog.viewerVote === 'down' ? 'default' : 'outline'}
                        className={model.blog.viewerVote === 'down' ? 'rounded-2xl bg-rose-600 text-white hover:bg-rose-700' : 'rounded-2xl border-rose-200 bg-white text-rose-700'}
                        disabled={isVoting}
                        onClick={() => {
                          void submitVote('down')
                        }}
                      >
                        <ThumbsDown className="size-4" />
                        {t('blog.vote.down')}
                      </Button>
                    </div>
                  </div>
                </div>
                <div className="mt-6 rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
                  <MarkdownDocument content={blogContentValue(model.blog.content)} />
                </div>
                <div className="mt-8 space-y-5">
                  <div>
                    <h2 className="text-lg font-semibold text-slate-950">{t('blog.comment.heading')}</h2>
                    <p className="mt-1 text-sm text-slate-500">{t('blog.comment.description')}</p>
                  </div>
                  <div className="space-y-3 rounded-3xl border border-slate-200 bg-white p-4">
                    <Textarea
                      value={commentContent}
                      className="min-h-28"
                      placeholder={t('blog.comment.placeholder')}
                      onChange={(event) => setCommentContent(event.target.value)}
                    />
                    {commentErrorMessage ? (
                      <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                        <AlertDescription className="text-rose-700">{commentErrorMessage}</AlertDescription>
                      </Alert>
                    ) : null}
                    <Button
                      type="button"
                      disabled={isSubmittingComment}
                      className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
                      onClick={() => {
                        void submitComment()
                      }}
                    >
                      {isSubmittingComment ? t('blog.comment.submitting') : t('blog.comment.submit')}
                    </Button>
                  </div>
                  <div className="space-y-3">
                    <h3 className="text-base font-semibold text-slate-950">{t('blog.comment.heading')}</h3>
                    {model.blog.comments.length === 0 ? (
                      <p className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-5 py-6 text-sm text-slate-500">
                        {t('blog.comment.empty')}
                      </p>
                    ) : (
                      <div className="space-y-3">
                        {model.blog.comments.filter((comment) => comment.parentId === null).map((comment) => renderComment(comment, 0))}
                      </div>
                    )}
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
