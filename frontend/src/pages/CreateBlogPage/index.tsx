import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { NotebookPen } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useCreateBlogAction } from './hooks/useCreateBlogAction'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { buildResourceVisibilityPolicy } from '@/pages/components/ResourceAccessEditorInput'
import { validateBlogFormDraft } from '@/pages/objects/BlogForm'
import { MarkdownEditorTabs } from '@/pages/components/MarkdownEditorTabs'
import { PageShell } from '@/pages/components/PageShell'
import { ResourceAccessEditor } from '@/pages/components/ResourceAccessEditor'
import { useBeforeUnloadPrompt } from '@/pages/hooks/useBeforeUnloadPrompt'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 创建博客页，维护标题、正文、可见性策略和 Markdown 预览 tab 草稿。
 * 存在未保存内容时注册离页提示，提交前按领域对象规则校验输入。
 */
export function CreateBlogPage() {
  const { t } = useI18n()
  usePageTitle(t('blog.create.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [baseAccess, setBaseAccess] = useState<BaseAccess>('public')
  const [grantedUsersInput, setGrantedUsersInput] = useState('')
  const [grantedGroupsInput, setGrantedGroupsInput] = useState('')
  const [contentTab, setContentTab] = useState<'write' | 'preview'>('write')
  const createBlogAction = useCreateBlogAction(t('blog.message.createFailed'))
  const accessPolicyResult = buildResourceVisibilityPolicy(baseAccess, grantedUsersInput, grantedGroupsInput)
  const accessPolicy = accessPolicyResult.ok ? accessPolicyResult.value : { baseAccess, viewerGrants: [] }
  const hasUnsavedChanges =
    title.trim().length > 0 ||
    content.trim().length > 0 ||
    baseAccess !== 'public' ||
    grantedUsersInput.trim().length > 0 ||
    grantedGroupsInput.trim().length > 0

  useBeforeUnloadPrompt(hasUnsavedChanges)

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  async function submit() {
    const validation = validateBlogFormDraft({
      title,
      content,
      baseAccess,
      grantedUsersInput,
      grantedGroupsInput,
    })
    if (!validation.ok) {
      createBlogAction.setErrorMessage(validation.message)
      return
    }

    await createBlogAction.submit(validation.request)
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
              textareaClassName="min-h-72 font-mono"
            />
            <p className="text-xs text-slate-500">{t('problem.create.markdownHelp')}</p>
          </div>

          <ResourceAccessEditor
            accessPolicy={accessPolicy}
            grantedUsersInput={grantedUsersInput}
            grantedGroupsInput={grantedGroupsInput}
            onBaseAccessChange={setBaseAccess}
            onGrantedUsersInputChange={setGrantedUsersInput}
            onGrantedGroupsInputChange={setGrantedGroupsInput}
          />

          {createBlogAction.errorMessage ? (
            <Alert variant="destructive">
              <AlertDescription>{createBlogAction.errorMessage}</AlertDescription>
            </Alert>
          ) : null}

          <Button
            type="button"
            disabled={createBlogAction.isSubmitting}
            variant="create"
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
