import { Link, Navigate, useParams } from 'react-router-dom'
import { NotebookPen, PenLine } from 'lucide-react'
import { useEffect, useState } from 'react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { parseUsername, type Username } from '@/features/auth/domain/auth'
import { blogIdValue, blogTitleValue, parseBlogId, type BlogId, type BlogSummary } from '@/features/blog/domain/blog'
import { acceptBlogProblemSubmission, linkBlogToProblem, listPendingProblemBlogs, unlinkBlogFromProblem } from '@/features/blog/api/blog-client'
import { useBlogListQuery } from '@/features/blog/hooks/use-blog-list-query'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import {
  formatProblemTitleDisplay,
  parseProblemSlug,
  problemSlugValue,
  useProblemTitleDisplayMode,
  type ProblemSlug,
} from '@/features/problem/domain/problem'
import { AppSectionBar } from '@/shared/components/app-section-bar'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
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

type BlogPageProps = {
  authorUsernameFilter?: Username
  problemSlugFilter?: ProblemSlug
}

export function BlogPage({ authorUsernameFilter, problemSlugFilter }: BlogPageProps = {}) {
  const { t } = useI18n()
  usePageTitle(t('blog.pageTitle'))
  const problemTitleDisplayMode = useProblemTitleDisplayMode()
  const { session: user, navigationIntent } = useSessionGuard()
  const model = useBlogListQuery(authorUsernameFilter ?? null, problemSlugFilter ?? null)
  const [linkBlogId, setLinkBlogId] = useState('')
  const [linkMessage, setLinkMessage] = useState('')
  const [isLinking, setIsLinking] = useState(false)
  const [pendingBlogs, setPendingBlogs] = useState<BlogSummary[]>([])
  const [isLoadingPending, setIsLoadingPending] = useState(false)
  const [pendingMessage, setPendingMessage] = useState('')
  const [activeReviewBlogId, setActiveReviewBlogId] = useState<number | null>(null)
  const canManageProblemLinks = problemSlugFilter !== undefined && Boolean(user?.problemManager)

  useEffect(() => {
    let cancelled = false
    if (!canManageProblemLinks || !problemSlugFilter) {
      setPendingBlogs([])
      return () => {
        cancelled = true
      }
    }

    setIsLoadingPending(true)
    setPendingMessage('')
    listPendingProblemBlogs(problemSlugFilter)
      .then((blogs) => {
        if (!cancelled) {
          setPendingBlogs(blogs)
          setIsLoadingPending(false)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setPendingBlogs([])
          setIsLoadingPending(false)
          setPendingMessage(t('blog.problem.pendingLoadFailed'))
        }
      })

    return () => {
      cancelled = true
    }
  }, [canManageProblemLinks, problemSlugFilter, model.blogs, t])

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const canCreateBlog = authorUsernameFilter === undefined && problemSlugFilter === undefined

  async function submitLinkBlog() {
    if (!problemSlugFilter) {
      return
    }

    const parsedBlogId = parseBlogId(Number(linkBlogId))
    if (!parsedBlogId.ok) {
      setLinkMessage(parsedBlogId.error)
      return
    }

    setIsLinking(true)
    setLinkMessage('')
    try {
      await linkBlogToProblem(problemSlugFilter, parsedBlogId.value)
      model.reload()
      setLinkBlogId('')
      setLinkMessage(t('blog.problem.linkCreated'))
    } catch {
      setLinkMessage(t('blog.problem.linkFailed'))
    } finally {
      setIsLinking(false)
    }
  }

  async function removeProblemLink(blogId: BlogId) {
    if (!problemSlugFilter || !window.confirm(t('blog.problem.unlinkConfirm'))) {
      return
    }

    try {
      await unlinkBlogFromProblem(problemSlugFilter, blogId)
      model.reload()
    } catch {
      setLinkMessage(t('blog.problem.unlinkFailed'))
    }
  }

  async function acceptPendingBlog(blogId: BlogId) {
    if (!problemSlugFilter) {
      return
    }

    setActiveReviewBlogId(blogIdValue(blogId))
    setPendingMessage('')
    try {
      await acceptBlogProblemSubmission(problemSlugFilter, blogId)
      model.reload()
      setPendingBlogs((blogs) => blogs.filter((blog) => blogIdValue(blog.id) !== blogIdValue(blogId)))
      setPendingMessage(t('blog.problem.accepted'))
    } catch {
      setPendingMessage(t('blog.problem.acceptFailed'))
    } finally {
      setActiveReviewBlogId(null)
    }
  }

  async function rejectPendingBlog(blogId: BlogId) {
    if (!problemSlugFilter || !window.confirm(t('blog.problem.rejectConfirm'))) {
      return
    }

    setActiveReviewBlogId(blogIdValue(blogId))
    setPendingMessage('')
    try {
      await unlinkBlogFromProblem(problemSlugFilter, blogId)
      setPendingBlogs((blogs) => blogs.filter((blog) => blogIdValue(blog.id) !== blogIdValue(blogId)))
      setPendingMessage(t('blog.problem.rejected'))
    } catch {
      setPendingMessage(t('blog.problem.rejectFailed'))
    } finally {
      setActiveReviewBlogId(null)
    }
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#fff7ed_0%,#eef6ff_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('blog.heading')}</h1>
          </div>

          <AncestorNavigation />
        </div>

        <AppSectionBar />

        <div className="space-y-6">
          {model.errorMessage ? (
            <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
              <AlertDescription className="text-rose-700">{model.errorMessage}</AlertDescription>
            </Alert>
          ) : null}

          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-orange-100 text-orange-700">
                    <NotebookPen className="size-5" />
                  </div>
                  <div>
                    <CardTitle className="text-xl text-slate-950">{t('blog.list.cardTitle')}</CardTitle>
                    <CardDescription>{t('blog.list.cardDescription')}</CardDescription>
                  </div>
                </div>
                {canCreateBlog ? (
                  <Button asChild className="rounded-2xl bg-orange-300 text-orange-950 hover:bg-orange-400">
                    <Link to="/blogs/new">
                      <PenLine className="size-4" />
                      {t('blog.list.create')}
                    </Link>
                  </Button>
                ) : null}
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              {canManageProblemLinks ? (
                <div className="rounded-3xl border border-orange-100 bg-orange-50 p-4">
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
                    <div className="space-y-2 sm:max-w-xs">
                      <label className="text-sm font-medium text-orange-950" htmlFor="blog-link-id">
                        {t('blog.problem.linkExisting')}
                      </label>
                      <Input
                        id="blog-link-id"
                        value={linkBlogId}
                        placeholder={t('blog.problem.linkPlaceholder')}
                        onChange={(event) => setLinkBlogId(event.target.value)}
                      />
                    </div>
                    <Button
                      type="button"
                      disabled={isLinking}
                      className="rounded-2xl bg-orange-300 text-orange-950 hover:bg-orange-400"
                      onClick={() => {
                        void submitLinkBlog()
                      }}
                    >
                      {isLinking ? t('common.loading') : t('blog.problem.linkSubmit')}
                    </Button>
                  </div>
                  {linkMessage ? <p className="mt-2 text-sm text-orange-800">{linkMessage}</p> : null}
                </div>
              ) : null}
              {canManageProblemLinks ? (
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
                        <article key={blogIdValue(blog.id)} className="rounded-2xl border border-amber-100 bg-white p-4">
                          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                            <div>
                              <Link className="font-semibold text-slate-950 hover:underline" to={`/blogs/${blogIdValue(blog.id)}`}>
                                {blogTitleValue(blog.title)}
                              </Link>
                              <p className="mt-1 text-xs uppercase tracking-[0.18em] text-slate-400">
                                <span>{t('common.createdByLabel')} </span>
                                <UserProfileLink className="inline-flex items-baseline gap-2 normal-case tracking-normal" user={blog.author} />
                              </p>
                            </div>
                            <div className="flex flex-wrap gap-2">
                              <Button
                                type="button"
                                disabled={activeReviewBlogId === blogIdValue(blog.id)}
                                className="rounded-2xl bg-emerald-300 text-emerald-950 hover:bg-emerald-400"
                                onClick={() => {
                                  void acceptPendingBlog(blog.id)
                                }}
                              >
                                {t('blog.problem.accept')}
                              </Button>
                              <Button
                                type="button"
                                variant="outline"
                                disabled={activeReviewBlogId === blogIdValue(blog.id)}
                                className="rounded-2xl border-rose-200 bg-white text-rose-700"
                                onClick={() => {
                                  void rejectPendingBlog(blog.id)
                                }}
                              >
                                {t('blog.problem.reject')}
                              </Button>
                            </div>
                          </div>
                        </article>
                      ))}
                    </div>
                  )}
                </div>
              ) : null}
              {model.isLoading ? (
                <p className="text-sm text-slate-500">{t('blog.list.loading')}</p>
              ) : model.blogs.length === 0 ? (
                <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center">
                  <p className="text-base font-medium text-slate-900">{t('blog.list.emptyTitle')}</p>
                  <p className="mt-2 text-sm leading-7 text-slate-600">{t('blog.list.emptyDescription')}</p>
                </div>
              ) : (
                model.blogs.map((blog) => (
                  <article key={blog.id} className="rounded-3xl border border-slate-200 bg-slate-50 p-5">
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                      <div className="flex gap-4">
                        <div>
                          <Link className="text-xl font-semibold text-slate-950 hover:underline" to={`/blogs/${blogIdValue(blog.id)}`}>
                            {blogTitleValue(blog.title)}
                          </Link>
                          <span className="ml-3 rounded-full bg-white px-3 py-1 text-xs font-semibold text-slate-600">
                            {t(`blog.visibility.${blog.visibility}`)}
                          </span>
                          {blog.relatedProblems.length > 0 ? (
                            <div className="mt-2 flex flex-wrap gap-2 text-sm text-slate-600">
                              <span>{t('blog.problem.linkedTo')}</span>
                              {blog.relatedProblems.slice(0, 3).map((problem) => (
                                <Link key={problemSlugValue(problem.slug)} className="font-semibold text-orange-700 hover:underline" to={`/problems/${problemSlugValue(problem.slug)}`}>
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
                        <p className={`text-sm font-semibold ${scoreClassName(blog.score)}`}>
                          {t('blog.vote.score', { score: String(blog.score) })}
                        </p>
                        <p className="text-sm text-slate-500">{formatDate(blog.createdAt)}</p>
                        {canManageProblemLinks ? (
                          <Button
                            type="button"
                            variant="outline"
                            className="rounded-2xl border-rose-200 bg-white text-rose-700"
                            onClick={() => {
                              void removeProblemLink(blog.id)
                            }}
                          >
                            {t('blog.problem.unlink')}
                          </Button>
                        ) : null}
                      </div>
                    </div>
                  </article>
                ))
              )}
            </CardContent>
          </Card>
        </div>
      </section>
    </main>
  )
}

export function UserBlogPage() {
  const { username } = useParams<{ username: string }>()
  const usernameResult = parseUsername(username ?? '')

  if (!usernameResult.ok) {
    return <Navigate replace to="/blogs" />
  }

  return <BlogPage authorUsernameFilter={usernameResult.value} />
}

export function ProblemBlogPage() {
  const { slug } = useParams<{ slug: string }>()
  const slugResult = parseProblemSlug(slug ?? '')

  if (!slugResult.ok) {
    return <Navigate replace to="/blogs" />
  }

  return <BlogPage problemSlugFilter={slugResult.value} />
}
