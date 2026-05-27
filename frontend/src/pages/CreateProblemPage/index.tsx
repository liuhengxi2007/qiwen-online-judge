import { useDeferredValue, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useNavigate } from 'react-router-dom'
import { FilePlus2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import { useSessionGuard } from '@/pages/hooks/use-session-guard'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { useCreateProblemPageModel } from './hooks/use-create-problem-page-model'
import { AppSectionBar } from '@/pages/components/app-section-bar'
import { MarkdownDocument } from '@/pages/components/markdown-document'
import { ResourceAccessEditor } from '@/pages/components/resource-access-editor'
import { AncestorNavigation } from '@/pages/components/ancestor-navigation'
import { useBeforeUnloadPrompt } from '@/pages/hooks/use-before-unload-prompt'
import { usePageTitle } from '@/pages/hooks/use-page-title'
import { useI18n } from '@/system/i18n/use-i18n'

export function CreateProblemPage() {
  const { t } = useI18n()
  usePageTitle(t('problem.create.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return <CreateProblemPageContent canCreate={user.siteManager || user.problemManager} />
}

function CreateProblemPageContent({ canCreate }: { canCreate: boolean }) {
  const { t } = useI18n()
  const othersSubmissionAccessOptions = [
    { value: 'none', label: t('problem.others.none.label'), description: t('problem.others.none.description') },
    { value: 'summary', label: t('problem.others.summary.label'), description: t('problem.others.summary.description') },
    { value: 'detail', label: t('problem.others.detail.label'), description: t('problem.others.detail.description') },
  ] as const
  const navigate = useNavigate()
  const model = useCreateProblemPageModel(canCreate)
  const [statementTab, setStatementTab] = useState<'write' | 'preview'>('write')
  const deferredStatement = useDeferredValue(model.statement)
  const hasUnsavedChanges =
    model.slug.trim().length > 0 ||
    model.title.trim().length > 0 ||
    model.statement.trim().length > 0 ||
    model.baseAccess !== 'owner_only' ||
    model.grantedUsersInput.trim().length > 0 ||
    model.grantedGroupsInput.trim().length > 0 ||
    model.managerUsersInput.trim().length > 0 ||
    model.managerGroupsInput.trim().length > 0 ||
    model.othersSubmissionAccess !== 'none'

  useBeforeUnloadPrompt(hasUnsavedChanges)

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#edf5f1_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('problem.create.heading')}</h1>
          </div>

          <AncestorNavigation />
        </div>

        <AppSectionBar />

        <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                <FilePlus2 className="size-5" />
              </div>
              <div>
                <CardTitle className="text-xl text-slate-950">{t('problem.create.cardTitle')}</CardTitle>
                <CardDescription>
                  {t('problem.create.cardDescription')}
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-5">
            {!canCreate ? (
              <Alert className="rounded-2xl border-amber-200 bg-amber-50/95">
                <AlertDescription className="text-amber-900">
                  {t('problem.create.permissionRequired')}
                </AlertDescription>
              </Alert>
            ) : null}
            <div className="space-y-2">
              <Label htmlFor="problem-slug">{t('problem.create.slug')}</Label>
              <Input
                id="problem-slug"
                value={model.slug}
                onChange={(event) => model.setSlug(event.target.value.toLowerCase())}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="problem-title">{t('problem.create.titleLabel')}</Label>
              <Input
                id="problem-title"
                value={model.title}
                onChange={(event) => model.setTitle(event.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="problem-statement">{t('problem.create.statement')}</Label>
              <Tabs value={statementTab} onValueChange={(value) => setStatementTab(value as 'write' | 'preview')}>
                <TabsList className="grid w-full grid-cols-2 rounded-2xl bg-slate-100">
                  <TabsTrigger value="write" className="rounded-xl">
                    {t('common.write')}
                  </TabsTrigger>
                  <TabsTrigger value="preview" className="rounded-xl">
                    {t('common.preview')}
                  </TabsTrigger>
                </TabsList>
                <TabsContent value="write" className="mt-3">
                  <Textarea
                    id="problem-statement"
                    value={model.statement}
                    className="min-h-64 !font-mono"
                    onChange={(event) => model.setStatement(event.target.value)}
                  />
                </TabsContent>
                <TabsContent value="preview" className="mt-3">
                  <div className="rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
                    {deferredStatement.trim() ? (
                      <MarkdownDocument content={deferredStatement} />
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
              grantedManagerUsersInput={model.managerUsersInput}
              grantedManagerGroupsInput={model.managerGroupsInput}
              onBaseAccessChange={model.setBaseAccess}
              onGrantedUsersInputChange={model.setGrantedUsersInput}
              onGrantedGroupsInputChange={model.setGrantedGroupsInput}
              onGrantedManagerUsersInputChange={model.setManagerUsersInput}
              onGrantedManagerGroupsInputChange={model.setManagerGroupsInput}
            />
            <div className="space-y-2">
              <Label htmlFor="problem-others-submission-access">{t('problem.create.othersSubmissionAccess')}</Label>
              <Select value={model.othersSubmissionAccess} onValueChange={model.setOthersSubmissionAccess}>
                <SelectTrigger id="problem-others-submission-access" className="rounded-2xl">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {othersSubmissionAccessOptions.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <p className="text-xs text-slate-500">
                {othersSubmissionAccessOptions.find((option) => option.value === model.othersSubmissionAccess)?.description}
              </p>
            </div>

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
                void model.submit().then((createdProblem) => {
                  if (createdProblem) {
                    void navigate(`/problems/${problemSlugValue(createdProblem.slug)}`)
                  }
                })
              }}
            >
              {model.isSubmitting ? t('problem.create.submitting') : t('problem.create.submit')}
            </Button>
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
