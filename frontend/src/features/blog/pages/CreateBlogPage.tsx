import { useDeferredValue, useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { NotebookPen } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import { createBlog } from '@/features/blog/api/blog-client'
import { blogIdValue, parseBlogContent, parseBlogTitle, type BlogType, type BlogVisibility } from '@/features/blog/domain/blog'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { listProblems } from '@/features/problem/api/problem-client'
import { parseProblemSlug, problemSlugValue, problemTitleValue, type ProblemSummary } from '@/features/problem/domain/problem'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { MarkdownDocument } from '@/shared/components/markdown-document'
import { SignedInUser } from '@/shared/components/signed-in-user'
import { useBeforeUnloadPrompt } from '@/shared/hooks/use-before-unload-prompt'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/i18n'

export function CreateBlogPage() {
  const { t } = useI18n()
  usePageTitle(t('blog.create.pageTitle'))
  const navigate = useNavigate()
  const { session: user, navigationIntent } = useSessionGuard()
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [visibility, setVisibility] = useState<BlogVisibility>('public')
  const [blogType, setBlogType] = useState<BlogType>('general')
  const [selectedProblemSlug, setSelectedProblemSlug] = useState('')
  const [problems, setProblems] = useState<ProblemSummary[]>([])
  const [contentTab, setContentTab] = useState<'write' | 'preview'>('write')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const deferredContent = useDeferredValue(content)
  const hasUnsavedChanges = title.trim().length > 0 || content.trim().length > 0 || blogType !== 'general'

  useBeforeUnloadPrompt(hasUnsavedChanges)

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

  async function submit() {
    const parsedTitle = parseBlogTitle(title)
    if (!parsedTitle.ok) {
      setErrorMessage(parsedTitle.error)
      return
    }

    const parsedContent = parseBlogContent(content)
    if (!parsedContent.ok) {
      setErrorMessage(parsedContent.error)
      return
    }

    const problemSlug =
      blogType === 'problem'
        ? parseProblemSlug(selectedProblemSlug)
        : { ok: true as const, value: null }
    if (!problemSlug.ok) {
      setErrorMessage(problemSlug.error)
      return
    }

    setIsSubmitting(true)
    setErrorMessage('')
    try {
      const createdBlog = await createBlog({
        title: parsedTitle.value,
        content: parsedContent.value,
        visibility,
        blogType,
        problemSlug: problemSlug.value,
      })
      navigate(`/blogs/${blogIdValue(createdBlog.id)}`)
    } catch {
      setErrorMessage(t('blog.message.createFailed'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#fff7ed_0%,#eef6ff_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-5xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('blog.create.heading')}</h1>
            <SignedInUser user={user} />
          </div>

          <AncestorNavigation />
        </div>

        <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-orange-100 text-orange-700">
                <NotebookPen className="size-5" />
              </div>
              <div>
                <CardTitle className="text-xl text-slate-950">{t('blog.create.cardTitle')}</CardTitle>
                <CardDescription>{t('blog.create.cardDescription')}</CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-5">
            <div className="space-y-2">
              <Label htmlFor="blog-title">{t('blog.create.titleLabel')}</Label>
              <Input
                id="blog-title"
                value={title}
                placeholder={t('blog.create.titlePlaceholder')}
                onChange={(event) => setTitle(event.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="blog-content">{t('blog.create.contentLabel')}</Label>
              <Tabs value={contentTab} onValueChange={(value) => setContentTab(value as 'write' | 'preview')}>
                <TabsList className="grid w-full grid-cols-2 rounded-2xl bg-slate-100">
                  <TabsTrigger value="write" className="rounded-xl">{t('common.write')}</TabsTrigger>
                  <TabsTrigger value="preview" className="rounded-xl">{t('common.preview')}</TabsTrigger>
                </TabsList>
                <TabsContent value="write" className="mt-3">
                  <Textarea
                    id="blog-content"
                    value={content}
                    className="min-h-72 !font-mono"
                    placeholder={t('blog.create.contentPlaceholder')}
                    onChange={(event) => setContent(event.target.value)}
                  />
                </TabsContent>
                <TabsContent value="preview" className="mt-3">
                  <div className="rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
                    {deferredContent.trim() ? (
                      <MarkdownDocument content={deferredContent} />
                    ) : (
                      <p className="text-sm text-slate-500">{t('common.nothingToPreview')}</p>
                    )}
                  </div>
                </TabsContent>
              </Tabs>
              <p className="text-xs text-slate-500">{t('problem.create.markdownHelp')}</p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="blog-visibility">{t('blog.create.visibilityLabel')}</Label>
              <Select value={visibility} onValueChange={(value) => setVisibility(value as BlogVisibility)}>
                <SelectTrigger id="blog-visibility" className="rounded-2xl border-slate-300 bg-white">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="public">{t('blog.visibility.public')}</SelectItem>
                  <SelectItem value="private">{t('blog.visibility.private')}</SelectItem>
                </SelectContent>
              </Select>
              <p className="text-xs text-slate-500">{t('blog.create.visibilityHelp')}</p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="blog-type">{t('blog.create.typeLabel')}</Label>
              <Select
                value={blogType}
                onValueChange={(value) => {
                  const nextType = value as BlogType
                  setBlogType(nextType)
                  if (nextType === 'general') {
                    setSelectedProblemSlug('')
                  }
                }}
              >
                <SelectTrigger id="blog-type" className="rounded-2xl border-slate-300 bg-white">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="general">{t('blog.type.general')}</SelectItem>
                  <SelectItem value="problem">{t('blog.type.problem')}</SelectItem>
                </SelectContent>
              </Select>
              <p className="text-xs text-slate-500">{t('blog.create.typeHelp')}</p>
            </div>

            {blogType === 'problem' ? (
              <div className="space-y-2">
                <Label htmlFor="blog-problem">{t('blog.create.problemLabel')}</Label>
                <Select value={selectedProblemSlug} onValueChange={setSelectedProblemSlug}>
                  <SelectTrigger id="blog-problem" className="rounded-2xl border-slate-300 bg-white">
                    <SelectValue placeholder={t('blog.create.problemPlaceholder')} />
                  </SelectTrigger>
                  <SelectContent>
                    {problems.map((problem) => (
                      <SelectItem key={problemSlugValue(problem.slug)} value={problemSlugValue(problem.slug)}>
                        {problemTitleValue(problem.title)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <p className="text-xs text-slate-500">{t('blog.create.problemHelp')}</p>
              </div>
            ) : null}

            {errorMessage ? (
              <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                <AlertDescription className="text-rose-700">{errorMessage}</AlertDescription>
              </Alert>
            ) : null}

            <Button
              type="button"
              disabled={isSubmitting}
              className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
              onClick={() => {
                void submit()
              }}
            >
              {isSubmitting ? t('blog.create.submitting') : t('blog.create.submit')}
            </Button>
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
