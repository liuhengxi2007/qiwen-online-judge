import { useDeferredValue, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useNavigate } from 'react-router-dom'
import { BookPlus } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import { useSessionGuard } from '@/pages/hooks/use-session-guard'
import { problemSetSlugValue } from '@/objects/problemset/problemset-parsers'
import { useCreateProblemSetPageModel } from './hooks/use-create-problemset-page-model'
import { AppSectionBar } from '@/pages/components/app-section-bar'
import { MarkdownDocument } from '@/pages/components/markdown-document'
import { ResourceAccessEditor } from '@/pages/components/resource-access-editor'
import { AncestorNavigation } from '@/pages/components/ancestor-navigation'
import { useBeforeUnloadPrompt } from '@/pages/hooks/use-before-unload-prompt'
import { usePageTitle } from '@/pages/hooks/use-page-title'
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
  const deferredDescription = useDeferredValue(model.description)
  const hasUnsavedChanges =
    model.slug.trim().length > 0 ||
    model.title.trim().length > 0 ||
    model.description.trim().length > 0 ||
    model.baseAccess !== 'owner_only' ||
    model.grantedUsersInput.trim().length > 0 ||
    model.grantedGroupsInput.trim().length > 0

  useBeforeUnloadPrompt(hasUnsavedChanges)

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#fdf8fb_0%,#f4edf7_48%,#ecf3fb_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('problemSet.create.heading')}</h1>
          </div>

          <AncestorNavigation />
        </div>

        <AppSectionBar />

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
                  <AlertDescription className="text-amber-900">
                    {t('problemSet.create.permissionRequired')}
                  </AlertDescription>
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
              <Tabs value={descriptionTab} onValueChange={(value) => setDescriptionTab(value as 'write' | 'preview')}>
                <TabsList className="grid w-full grid-cols-2 rounded-2xl bg-slate-100">
                  <TabsTrigger value="write" className="rounded-xl">{t('common.write')}</TabsTrigger>
                  <TabsTrigger value="preview" className="rounded-xl">{t('common.preview')}</TabsTrigger>
                </TabsList>
                <TabsContent value="write" className="mt-3">
                  <Textarea
                    id="problem-set-description"
                    value={model.description}
                    className="min-h-48 !font-mono"
                    onChange={(event) => model.setDescription(event.target.value)}
                  />
                </TabsContent>
                <TabsContent value="preview" className="mt-3">
                  <div className="rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
                    {deferredDescription.trim() ? (
                      <MarkdownDocument content={deferredDescription} />
                    ) : (
                      <p className="text-sm text-slate-500">{t('common.nothingToPreview')}</p>
                    )}
                  </div>
                </TabsContent>
              </Tabs>
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
      </section>
    </main>
  )
}
