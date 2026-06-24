import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { NotebookPen, PenLine } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { buildPageNumbers, calculateTotalPages, parsePositivePage } from '@/pages/objects/Pagination'
import type { Username } from '@/objects/user/Username'
import { PaginationControls } from '@/pages/components/PaginationControls'
import { PageShell } from '@/pages/components/PageShell'
import { usePageSearchParamCorrection } from '@/pages/hooks/usePageSearchParamCorrection'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { useI18n } from '@/system/i18n/use-i18n'
import { BlogProblemLinkManagerCard } from './components/BlogProblemLinkManagerCard'
import { BlogSummaryCard } from './components/BlogSummaryCard'
import { PendingProblemBlogsCard } from './components/PendingProblemBlogsCard'
import { useBlogPageModel } from './hooks/useBlogPageModel'

/**
 * 博客列表内容属性，可限定作者或题目筛选。
 */
type BlogListPageContentProps = {
  authorUsernameFilter?: Username
  problemSlugFilter?: ProblemSlug
}

const blogsPerPage = 10

/**
 * 博客列表页面主体，负责会话守卫、分页查询、待关联题解和博客列表展示。
 */
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
  const onPageChange = (page: number) => {
    const nextSearchParams = new URLSearchParams(searchParams)
    nextSearchParams.set('page', String(page))
    setSearchParams(nextSearchParams)
  }

  return (
    <PageShell title={t('blog.heading')} mainClassName="bg-[linear-gradient(180deg,#fff7ed_0%,#eef6ff_100%)]">
      <div className="space-y-6">
        {model.errorMessage ? (
          <Alert variant="destructive">
            <AlertDescription>{model.errorMessage}</AlertDescription>
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
                <Button asChild variant="create">
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
                        variant="destructiveOutline"
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
              <PaginationControls
                currentPage={currentPage}
                pageNumbers={pageNumbers}
                totalPages={totalPages}
                previousLabel={t('common.pagination.previous')}
                nextLabel={t('common.pagination.next')}
                onPageChange={onPageChange}
              />
            ) : null}
          </CardContent>
        </Card>
      </div>
    </PageShell>
  )
}
