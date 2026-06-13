import { useEffect } from 'react'
import { useNavigate, useParams, Navigate } from 'react-router-dom'
import { NotebookPen } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { usernameValue } from '@/objects/user/Username'
import { parseBlogId } from '@/objects/blog/BlogId'
import { blogTitleValue } from '@/objects/blog/BlogTitle'
import { useBlogDetailQuery } from './hooks/useBlogDetailQuery'
import { useBlogDetailPageModel } from './hooks/useBlogDetailPageModel'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { useProblemTitleDisplayMode } from '@/pages/hooks/useProblemTitleDisplay'
import { PageShell } from '@/pages/components/PageShell'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'

import { BlogArticleContent } from './components/BlogArticleContent'
import { BlogCommentsSection } from './components/BlogCommentsSection'
import { BlogEditForm } from './components/BlogEditForm'
import { BlogMetaVoteBar } from './components/BlogMetaVoteBar'
import { BlogOwnerActions } from './components/BlogOwnerActions'

/**
 * 博客详情页，负责登录保护、博客 id 解析、详情加载以及 hash 锚点滚动。
 * 作者可在本页编辑、删除或提交到题目，普通读者只看到正文、投票和评论入口。
 */
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
    <PageShell title={t('blog.detail.heading')} mainClassName="bg-[linear-gradient(180deg,#fff7ed_0%,#eef6ff_100%)]">
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
    </PageShell>
  )
}
