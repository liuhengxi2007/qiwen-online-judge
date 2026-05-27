import { useEffect } from 'react'
import { useNavigate, useParams, Navigate } from 'react-router-dom'
import { NotebookPen } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { usernameValue } from '@/objects/user/Username'
import { parseBlogId } from '@/objects/blog/BlogId'
import { blogTitleValue } from '@/objects/blog/BlogTitle'
import { useBlogDetailQuery } from './hooks/use-blog-detail-query'
import { useBlogDetailPageModel } from './hooks/use-blog-detail-page-model'
import { useSessionGuard } from '@/pages/hooks/use-session-guard'
import { useProblemTitleDisplayMode } from '@/pages/hooks/use-problem-title-display'
import { AppSectionBar } from '@/pages/components/app-section-bar'
import { AncestorNavigation } from '@/pages/components/ancestor-navigation'
import { usePageTitle } from '@/pages/hooks/use-page-title'
import { useI18n } from '@/system/i18n/use-i18n'

import { BlogArticleContent } from './components/BlogArticleContent'
import { BlogCommentsSection } from './components/BlogCommentsSection'
import { BlogEditForm } from './components/BlogEditForm'
import { BlogMetaVoteBar } from './components/BlogMetaVoteBar'
import { BlogOwnerActions } from './components/BlogOwnerActions'

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

  useEffect(() => {
    if (!query.blog || !window.location.hash) {
      return
    }

    const element = document.getElementById(window.location.hash.slice(1))
    if (!element) {
      return
    }

    window.requestAnimationFrame(() => {
      element.scrollIntoView({ block: 'center' })
    })
  }, [query.blog])

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
                  <BlogOwnerActions blog={query.blog} model={model} />
                ) : null}

                {model.isEditingBlog ? <BlogEditForm model={model} /> : null}

                <BlogMetaVoteBar blog={query.blog} model={model} problemTitleDisplayMode={problemTitleDisplayMode} />
                <BlogArticleContent blog={query.blog} />
                <BlogCommentsSection blog={query.blog} currentUsername={usernameValue(user.username)} model={model} />
              </article>
            ) : null}
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
