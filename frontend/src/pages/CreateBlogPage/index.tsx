import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { NotebookPen } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useCreateBlogAction } from './hooks/useCreateBlogAction'
import { parseBlogContent } from '@/objects/blog/BlogContent'
import { parseBlogTitle } from '@/objects/blog/BlogTitle'
import type { BlogVisibility } from '@/objects/blog/BlogVisibility'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { MarkdownEditorTabs } from '@/pages/components/MarkdownEditorTabs'
import { PageShell } from '@/pages/components/PageShell'
import { useBeforeUnloadPrompt } from '@/pages/hooks/useBeforeUnloadPrompt'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'

export function CreateBlogPage() {
  const { t } = useI18n()
  usePageTitle(t('blog.create.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [visibility, setVisibility] = useState<BlogVisibility>('public')
  const [contentTab, setContentTab] = useState<'write' | 'preview'>('write')
  const createBlogAction = useCreateBlogAction(t('blog.message.createFailed'))
  const hasUnsavedChanges = title.trim().length > 0 || content.trim().length > 0

  useBeforeUnloadPrompt(hasUnsavedChanges)

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  async function submit() {
    const parsedTitle = parseBlogTitle(title)
    if (!parsedTitle.ok) {
      createBlogAction.setErrorMessage(parsedTitle.error)
      return
    }

    const parsedContent = parseBlogContent(content)
    if (!parsedContent.ok) {
      createBlogAction.setErrorMessage(parsedContent.error)
      return
    }

    await createBlogAction.submit({
      title: parsedTitle.value,
      content: parsedContent.value,
      visibility,
    })
  }

  return (
    <PageShell title={t('blog.create.heading')} mainClassName="bg-[linear-gradient(180deg,#fff7ed_0%,#eef6ff_100%)]">
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
            <Input id="blog-title" value={title} onChange={(event) => setTitle(event.target.value)} />
          </div>

          <div className="space-y-2">
            <Label htmlFor="blog-content">{t('blog.create.contentLabel')}</Label>
            <MarkdownEditorTabs
              textareaId="blog-content"
              value={content}
              tab={contentTab}
              onTabChange={setContentTab}
              onValueChange={setContent}
              textareaClassName="min-h-72 !font-mono"
            />
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

          {createBlogAction.errorMessage ? (
            <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
              <AlertDescription className="text-rose-700">{createBlogAction.errorMessage}</AlertDescription>
            </Alert>
          ) : null}

          <Button
            type="button"
            disabled={createBlogAction.isSubmitting}
            className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
            onClick={() => {
              void submit()
            }}
          >
            {createBlogAction.isSubmitting ? t('blog.create.submitting') : t('blog.create.submit')}
          </Button>
        </CardContent>
      </Card>
    </PageShell>
  )
}
