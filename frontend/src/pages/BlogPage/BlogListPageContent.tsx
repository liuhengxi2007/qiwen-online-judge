import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { NotebookPen, PenLine } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { buildPageNumbers, calculateTotalPages, parsePositivePage } from '@/objects/shared/pagination'
import type { Username } from '@/objects/user/Username'
import { AncestorNavigation } from '@/pages/components/ancestor-navigation'
import { AppSectionBar } from '@/pages/components/app-section-bar'
import { usePageSearchParamCorrection } from '@/pages/hooks/use-page-search-param-correction'
import { usePageTitle } from '@/pages/hooks/use-page-title'
import { useSessionGuard } from '@/pages/hooks/use-session-guard'
import { useI18n } from '@/system/i18n/use-i18n'
import { BlogProblemLinkManagerCard } from './components/blog-problem-link-manager-card'
import { BlogSummaryCard } from './components/blog-summary-card'
import { PendingProblemBlogsCard } from './components/pending-problem-blogs-card'
import { useBlogPageModel } from './hooks/use-blog-page-model'

type BlogListPageContentProps = {
  authorUsernameFilter?: Username
  problemSlugFilter?: ProblemSlug
}

const blogsPerPage = 10

export function BlogListPageContent({ authorUsernameFilter, problemSlugFilter }: BlogListPageContentProps = {}) {
  const { t } = useI18n()
  usePageTitle(t('blog.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const canManageProblemLinks = problemSlugFilter !== undefined && Boolean(user?.problemManager)
  const [searchParams, setSearchParams] = useSearchParams()
  const currentPage = parsePositivePage(searchParams.get('page'))
  const model = useBlogPageModel({ authorUsernameFilter, problemSlugFilter, canManageProblemLinks, pageRequest: { page: currentPage, pageSize: blogsPerPage } })
  const totalPages = calculateTotalPages(model.totalItems, model.pageSize)
  const pageNumbers = buildPageNumbers(currentPage, totalPages)

  usePageSearchParamCorrection({
    currentPage,
    totalPages,
    isLoading: model.isLoading,
    setSearchParams,
  })

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const canCreateBlog = authorUsernameFilter === undefined && problemSlugFilter === undefined

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
                <BlogProblemLinkManagerCard
                  linkBlogId={model.linkBlogId}
                  linkMessage={model.linkMessage}
                  isLinking={model.isLinking}
                  onLinkBlogIdChange={model.setLinkBlogId}
                  onSubmit={() => void model.submitLinkBlog()}
                />
              ) : null}

              {canManageProblemLinks ? (
                <PendingProblemBlogsCard
                  pendingBlogs={model.pendingBlogs}
                  isLoadingPending={model.isLoadingPending}
                  pendingMessage={model.pendingMessage}
                  activeReviewBlogId={model.activeReviewBlogId}
                  onAccept={(blog) => void model.acceptPendingBlog(blog)}
                  onReject={(blog) => void model.rejectPendingBlog(blog)}
                />
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
                  <BlogSummaryCard
                    key={blog.id}
                    blog={blog}
                    actions={
                      canManageProblemLinks ? (
                        <Button
                          type="button"
                          variant="outline"
                          className="rounded-2xl border-rose-200 bg-white text-rose-700"
                          onClick={() => void model.removeProblemLink(blog.id)}
                        >
                          {t('blog.problem.unlink')}
                        </Button>
                      ) : null
                    }
                  />
                ))
              )}
              {!model.isLoading && model.blogs.length > 0 && totalPages > 1 ? (
                <div className="flex flex-wrap items-center justify-center gap-2 pt-4">
                  <Button type="button" variant="outline" className="rounded-2xl border-slate-300 bg-white" disabled={currentPage === 1} onClick={() => {
                    const nextSearchParams = new URLSearchParams(searchParams)
                    nextSearchParams.set('page', String(Math.max(1, currentPage - 1)))
                    setSearchParams(nextSearchParams)
                  }}>{t('common.pagination.previous')}</Button>
                  {pageNumbers.map((page) => (
                    <Button key={page} type="button" variant={page === currentPage ? 'default' : 'outline'} className={page === currentPage ? 'rounded-2xl bg-slate-950 text-white' : 'rounded-2xl border-slate-300 bg-white'} onClick={() => {
                      const nextSearchParams = new URLSearchParams(searchParams)
                      nextSearchParams.set('page', String(page))
                      setSearchParams(nextSearchParams)
                    }}>{page}</Button>
                  ))}
                  <Button type="button" variant="outline" className="rounded-2xl border-slate-300 bg-white" disabled={currentPage === totalPages} onClick={() => {
                    const nextSearchParams = new URLSearchParams(searchParams)
                    nextSearchParams.set('page', String(Math.min(totalPages, currentPage + 1)))
                    setSearchParams(nextSearchParams)
                  }}>{t('common.pagination.next')}</Button>
                </div>
              ) : null}
            </CardContent>
          </Card>
        </div>
      </section>
    </main>
  )
}
