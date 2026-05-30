import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useNavigate } from 'react-router-dom'
import { BookPlus } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { problemSetSlugValue } from '@/objects/problemset/ProblemSetSlug'
import { useCreateProblemSetPageModel } from './hooks/useCreateProblemSetPageModel'
import { MarkdownEditorTabs } from '@/pages/components/MarkdownEditorTabs'
import { PageShell } from '@/pages/components/PageShell'
import { ResourceAccessEditor } from '@/pages/components/ResourceAccessEditor'
import { useBeforeUnloadPrompt } from '@/pages/hooks/useBeforeUnloadPrompt'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'

export function CreateProblemSetPage() {
  const { t } = useI18n()
  usePageTitle(t('problemSet.create.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return <CreateProblemSetPageContent canCreate={user.siteManager || user.problemManager} />
}

function CreateProblemSetPageContent({ canCreate }: { canCreate: boolean }) {
  const { t } = useI18n()
  const navigate = useNavigate()
  const model = useCreateProblemSetPageModel(canCreate)
  const [descriptionTab, setDescriptionTab] = useState<'write' | 'preview'>('write')
  const hasUnsavedChanges =
    model.slug.trim().length > 0 ||
    model.title.trim().length > 0 ||
    model.description.trim().length > 0 ||
    model.baseAccess !== 'owner_only' ||
    model.grantedUsersInput.trim().length > 0 ||
    model.grantedGroupsInput.trim().length > 0

  useBeforeUnloadPrompt(hasUnsavedChanges)

  return (
    <PageShell
      title={t('problemSet.create.heading')}
      mainClassName="bg-[linear-gradient(180deg,#fdf8fb_0%,#f4edf7_48%,#ecf3fb_100%)]"
    >
      <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-rose-100 text-rose-700">
              <BookPlus className="size-5" />
            </div>
            <div>
              <CardTitle className="text-xl text-slate-950">{t('problemSet.create.cardTitle')}</CardTitle>
              <CardDescription>{t('problemSet.create.cardDescription')}</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-5">
          {!canCreate ? (
            <Alert className="rounded-2xl border-amber-200 bg-amber-50/95">
              <AlertDescription className="text-amber-900">{t('problemSet.create.permissionRequired')}</AlertDescription>
            </Alert>
          ) : null}
          <div className="space-y-2">
            <Label htmlFor="problem-set-slug">{t('problemSet.create.slug')}</Label>
            <Input
              id="problem-set-slug"
              value={model.slug}
              onChange={(event) => model.setSlug(event.target.value.toLowerCase())}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="problem-set-title">{t('problemSet.create.titleLabel')}</Label>
            <Input
              id="problem-set-title"
              value={model.title}
              onChange={(event) => model.setTitle(event.target.value)}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="problem-set-description">{t('problemSet.create.descriptionLabel')}</Label>
            <MarkdownEditorTabs
              textareaId="problem-set-description"
              value={model.description}
              tab={descriptionTab}
              onTabChange={setDescriptionTab}
              onValueChange={model.setDescription}
              textareaClassName="min-h-48 !font-mono"
            />
            <p className="text-xs text-slate-500">{t('problem.create.markdownHelp')}</p>
          </div>

          <ResourceAccessEditor
            accessPolicy={model.accessPolicy}
            grantedUsersInput={model.grantedUsersInput}
            grantedGroupsInput={model.grantedGroupsInput}
            onBaseAccessChange={model.setBaseAccess}
            onGrantedUsersInputChange={model.setGrantedUsersInput}
            onGrantedGroupsInputChange={model.setGrantedGroupsInput}
          />

          {model.errorMessage ? (
            <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
              <AlertDescription className="text-rose-700">{model.errorMessage}</AlertDescription>
            </Alert>
          ) : null}
          {model.successMessage ? (
            <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
              <AlertDescription className="text-emerald-700">{model.successMessage}</AlertDescription>
            </Alert>
          ) : null}
          <Button
            type="button"
            disabled={model.isSubmitting || !canCreate}
            className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
            onClick={() => {
              void model.submit().then((createdProblemSet) => {
                if (createdProblemSet) {
                  void navigate(`/problem-sets/${problemSetSlugValue(createdProblemSet.slug)}`)
                }
              })
            }}
          >
            {model.isSubmitting ? t('problemSet.create.submitting') : t('problemSet.create.submit')}
          </Button>
        </CardContent>
      </Card>
    </PageShell>
  )
}
