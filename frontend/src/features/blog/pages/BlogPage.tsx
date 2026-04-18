import { Link, Navigate, useParams } from 'react-router-dom'
import { NotebookPen, PenLine } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { parseUsername, usernameValue, type Username } from '@/features/auth/domain/auth'
import { blogIdValue, blogTitleValue } from '@/features/blog/domain/blog'
import { useBlogListQuery } from '@/features/blog/hooks/use-blog-list-query'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { parseProblemSlug, problemSlugValue, problemTitleValue, type ProblemSlug } from '@/features/problem/domain/problem'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
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

type BlogPageProps = {
  authorUsernameFilter?: Username
  problemSlugFilter?: ProblemSlug
}

export function BlogPage({ authorUsernameFilter, problemSlugFilter }: BlogPageProps = {}) {
  const { t } = useI18n()
  usePageTitle(t('blog.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const model = useBlogListQuery(authorUsernameFilter ?? null, problemSlugFilter ?? null)

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const canCreateBlog = !authorUsernameFilter || usernameValue(authorUsernameFilter) === usernameValue(user.username)

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#fff7ed_0%,#eef6ff_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-5xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('blog.heading')}</h1>
            <SignedInUser user={user} />
          </div>

          <AncestorNavigation />
        </div>

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
                          <span className="ml-2 rounded-full bg-white px-3 py-1 text-xs font-semibold text-slate-600">
                            {t(`blog.type.${blog.blogType}`)}
                          </span>
                          {blog.problemSlug !== null && blog.problemTitle !== null ? (
                            <p className="mt-2 text-sm text-slate-600">
                              {t('blog.problem.linkedTo')}{' '}
                              <Link className="font-semibold text-orange-700 hover:underline" to={`/problems/${problemSlugValue(blog.problemSlug)}`}>
                                {problemTitleValue(blog.problemTitle)}
                              </Link>
                            </p>
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
