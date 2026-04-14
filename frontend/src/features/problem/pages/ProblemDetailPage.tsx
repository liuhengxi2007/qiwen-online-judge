import { useDeferredValue, useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { Database, PencilLine, ScrollText, Send, ShieldCheck, Trash2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import { Input } from '@/components/ui/input'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import {
  parseProblemSlug,
  problemSlugValue,
  problemStatementTextValue,
  problemTitleValue,
} from '@/features/problem/domain/problem'
import { useProblemDetailPageModel } from '@/features/problem/hooks/use-problem-detail-page-model'
import { ConfirmActionDialog } from '@/shared/components/confirm-action-dialog'
import { MarkdownDocument } from '@/shared/components/markdown-document'
import { ResourceAccessEditor } from '@/shared/components/resource-access-editor'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import {
  grantedGroupsInputFromAccessPolicy,
  grantedManagerGroupsInputFromAccessPolicy,
  grantedManagerUsersInputFromAccessPolicy,
  grantedUsersInputFromAccessPolicy,
  normalizeAccessSubjectInput,
} from '@/shared/domain/resource-access-input'
import { resourceAccessBadgeLabel, resourceAccessSummary } from '@/shared/domain/resource-lifecycle'
import { useBeforeUnloadPrompt } from '@/shared/hooks/use-before-unload-prompt'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/i18n'

export function ProblemDetailPage() {
  const { t } = useI18n()
  const othersSubmissionAccessOptions = [
    { value: 'none', label: t('problem.others.none.label'), description: t('problem.others.none.description') },
    { value: 'summary', label: t('problem.others.summary.label'), description: t('problem.others.summary.description') },
    { value: 'detail', label: t('problem.others.detail.label'), description: t('problem.others.detail.description') },
  ] as const
  usePageTitle(t('problem.detail.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const navigate = useNavigate()
  const { slug } = useParams<{ slug: string }>()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const slugResult = parseProblemSlug(slug ?? '')
  if (!slugResult.ok) {
    return <Navigate replace to="/problems" />
  }

  const model = useProblemDetailPageModel(slugResult.value)
  const canManageProblem = model.canManage
  const [managementPanel, setManagementPanel] = useState<'edit' | 'access' | null>(null)
  const [statementTab, setStatementTab] = useState<'write' | 'preview'>('write')
  const deferredStatement = useDeferredValue(model.statement)
  const hasUnsavedChanges =
    model.problem !== null &&
    (model.title !== problemTitleValue(model.problem.title) ||
      model.statement !== problemStatementTextValue(model.problem.statement) ||
      model.timeLimitMs !== model.problem.timeLimitMs ||
      model.spaceLimitMb !== model.problem.spaceLimitMb ||
      model.baseAccess !== model.problem.accessPolicy.baseAccess ||
      normalizeAccessSubjectInput(model.grantedUsersInput) !==
        grantedUsersInputFromAccessPolicy(model.problem.accessPolicy) ||
      normalizeAccessSubjectInput(model.grantedGroupsInput) !==
        grantedGroupsInputFromAccessPolicy(model.problem.accessPolicy) ||
      normalizeAccessSubjectInput(model.managerUsersInput) !==
        grantedManagerUsersInputFromAccessPolicy(model.problem.accessPolicy) ||
      normalizeAccessSubjectInput(model.managerGroupsInput) !==
        grantedManagerGroupsInputFromAccessPolicy(model.problem.accessPolicy) ||
      model.othersSubmissionAccess !== model.problem.othersSubmissionAccess)

  useBeforeUnloadPrompt(hasUnsavedChanges)

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#edf5f1_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('problem.detail.heading')}</h1>
            <p className="text-sm text-slate-600">
              {t('common.signedInAs', {
                displayName: displayNameValue(user.displayName),
                username: usernameValue(user.username),
              })}
            </p>
          </div>

          <AncestorNavigation />
        </div>

        {!model.isLoading && !model.problem && model.loadErrorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{model.loadErrorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {model.isLoading ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">{t('problem.detail.loading')}</CardContent>
          </Card>
        ) : model.problem ? (
          <div className="space-y-6">
            <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
              <CardHeader>
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div className="flex items-center gap-3">
                    <div className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                      <ScrollText className="size-5" />
                    </div>
                    <div>
                      <CardTitle className="text-2xl text-slate-950">{problemTitleValue(model.problem.title)}</CardTitle>
                      <CardDescription className="mt-2 font-mono text-sm text-slate-500">
                        {problemSlugValue(model.problem.slug)}
                      </CardDescription>
                    </div>
                  </div>

                  <div className="flex flex-wrap gap-3">
                    <Button
                      asChild
                      variant="outline"
                      className="rounded-2xl border-emerald-300 bg-white text-emerald-900 hover:bg-emerald-50"
                    >
                      <Link to={`/problems/${problemSlugValue(model.problem.slug)}/submit`}>
                        <Send className="size-4" />
                        {t('problem.detail.submitCode')}
                      </Link>
                    </Button>
                    {canManageProblem ? (
                      <>
                      <Button
                        asChild
                        variant="outline"
                        className="rounded-2xl border-emerald-300 bg-white text-emerald-900 hover:bg-emerald-50"
                      >
                        <Link to={`/problems/${problemSlugValue(model.problem.slug)}/data`}>
                          <Database className="size-4" />
                          {t('problem.detail.manageData')}
                        </Link>
                      </Button>
                      <Button
                        type="button"
                        variant={managementPanel === 'edit' ? 'default' : 'outline'}
                        className={
                          managementPanel === 'edit'
                            ? 'rounded-2xl bg-emerald-300 text-emerald-950 hover:bg-emerald-400'
                            : 'rounded-2xl border-emerald-300 bg-white text-emerald-900 hover:bg-emerald-50'
                        }
                        onClick={() => {
                          setManagementPanel((currentPanel) => (currentPanel === 'edit' ? null : 'edit'))
                        }}
                      >
                        <PencilLine className="size-4" />
                        {t('problem.detail.edit')}
                      </Button>
                      <Button
                        type="button"
                        variant={managementPanel === 'access' ? 'default' : 'outline'}
                        className={
                          managementPanel === 'access'
                            ? 'rounded-2xl bg-emerald-300 text-emerald-950 hover:bg-emerald-400'
                            : 'rounded-2xl border-emerald-300 bg-white text-emerald-900 hover:bg-emerald-50'
                        }
                        onClick={() => {
                          setManagementPanel((currentPanel) => (currentPanel === 'access' ? null : 'access'))
                        }}
                      >
                        <ShieldCheck className="size-4" />
                        {t('problem.detail.accessManagement')}
                      </Button>
                      </>
                    ) : null}
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-5">
                <div className="flex flex-wrap items-center gap-3">
                  <Badge variant="secondary">{resourceAccessBadgeLabel(model.problem.accessPolicy)}</Badge>
                </div>
                <div className="rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
                  <MarkdownDocument content={problemStatementTextValue(model.problem.statement)} />
                </div>
                <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
                  {t('problem.createdBy', { username: usernameValue(model.problem.creatorUsername) })}
                </p>
              </CardContent>
            </Card>

          </div>
        ) : null}
      </section>

      <Dialog
        open={canManageProblem && managementPanel === 'edit'}
        onOpenChange={(open) => {
          setManagementPanel(open ? 'edit' : null)
        }}
      >
        <DialogContent
          className="max-h-[calc(100vh-2rem)] max-w-4xl overflow-y-auto rounded-[2rem] border-slate-200 bg-white p-0 shadow-[0_28px_90px_rgba(15,23,42,0.22)]"
          onOpenAutoFocus={(event) => {
            event.preventDefault()
          }}
        >
          <DialogHeader className="border-b border-slate-200 px-7 py-6 sm:px-8">
            <DialogTitle className="flex items-center gap-3 text-2xl text-slate-950">
              <span className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                <PencilLine className="size-5" />
              </span>
              {t('problem.detail.editDialogTitle')}
            </DialogTitle>
            <DialogDescription className="text-sm leading-7 text-slate-600">
              {t('problem.detail.editDialogDescription')}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-6 px-7 py-7 sm:px-8">
            <div className="space-y-2">
              <Label htmlFor="problem-title">{t('problem.create.titleLabel')}</Label>
              <Input id="problem-title" value={model.title} onChange={(event) => model.setTitle(event.target.value)} />
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
                    className="min-h-64 !font-mono"
                    value={model.statement}
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
            <div className="grid gap-5 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="problem-time-limit">{t('problem.detail.timeLimit')}</Label>
                <Input
                  id="problem-time-limit"
                  type="number"
                  min={1}
                  value={model.timeLimitMs}
                  onChange={(event) => {
                    model.setTimeLimitMs(Number(event.target.value))
                  }}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="problem-space-limit">{t('problem.detail.spaceLimit')}</Label>
                <Input
                  id="problem-space-limit"
                  type="number"
                  min={1}
                  value={model.spaceLimitMb}
                  onChange={(event) => {
                    model.setSpaceLimitMb(Number(event.target.value))
                  }}
                />
              </div>
            </div>
            <Button
              type="button"
              className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
              disabled={model.isSaving}
              onClick={() => {
                void model.saveContent()
              }}
            >
              {model.isSaving ? t('problem.detail.savingContent') : t('problem.detail.saveContent')}
            </Button>
            {model.contentErrorMessage ? (
              <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                <AlertDescription className="text-rose-700">{model.contentErrorMessage}</AlertDescription>
              </Alert>
            ) : null}
            {model.contentSuccessMessage ? (
              <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
                <AlertDescription className="text-emerald-700">{model.contentSuccessMessage}</AlertDescription>
              </Alert>
            ) : null}

            <div className="rounded-[1.75rem] border border-rose-200 bg-rose-50/60 p-6">
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-rose-100 text-rose-700">
                  <Trash2 className="size-5" />
                </div>
                <div>
                  <h2 className="text-xl font-semibold text-rose-950">{t('problem.detail.deleteTitle')}</h2>
                  <p className="text-sm text-rose-900/80">
                    {t('problem.detail.deleteDescription')}
                  </p>
                </div>
              </div>
              <div className="mt-5">
                <ConfirmActionDialog
                  title={t('problem.detail.deleteConfirmTitle')}
                  description={t('problem.detail.deleteConfirmDescription')}
                  confirmLabel={model.isDeleting ? t('problem.detail.deletingAction') : t('problem.detail.deleteAction')}
                  destructive
                  onConfirm={() => {
                    void model.deleteCurrentProblem().then((deleted) => {
                      if (deleted) {
                        void navigate('/problems')
                      }
                    })
                  }}
                  trigger={
                    <Button
                      type="button"
                      variant="outline"
                      className="rounded-2xl border-rose-300 bg-white text-rose-700 hover:bg-rose-100 hover:text-rose-800"
                      disabled={model.isDeleting}
                    >
                      {model.isDeleting ? t('problem.detail.deletingAction') : t('problem.detail.deleteAction')}
                    </Button>
                  }
                />
              </div>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      <Dialog
        open={canManageProblem && managementPanel === 'access'}
        onOpenChange={(open) => {
          setManagementPanel(open ? 'access' : null)
        }}
      >
        <DialogContent
          className="max-h-[calc(100vh-2rem)] max-w-3xl overflow-y-auto rounded-[2rem] border-slate-200 bg-white p-0 shadow-[0_28px_90px_rgba(15,23,42,0.22)]"
          onOpenAutoFocus={(event) => {
            event.preventDefault()
          }}
        >
          <DialogHeader className="border-b border-slate-200 px-7 py-6 sm:px-8">
            <DialogTitle className="flex items-center gap-3 text-2xl text-slate-950">
              <span className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                <ShieldCheck className="size-5" />
              </span>
              {t('problem.detail.accessDialogTitle')}
            </DialogTitle>
            <DialogDescription className="text-sm leading-7 text-slate-600">
              {t('problem.detail.accessDialogDescription')}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-5 px-7 py-7 sm:px-8">
            <p className="text-sm text-slate-600">{resourceAccessSummary(model.problem?.accessPolicy ?? model.accessPolicy)}</p>
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
            <Button
              type="button"
              className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
              disabled={model.isSaving}
              onClick={() => {
                void model.saveAccess()
              }}
            >
              {model.isSaving ? t('problem.detail.savingAccess') : t('problem.detail.saveAccess')}
            </Button>
            {model.accessErrorMessage ? (
              <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                <AlertDescription className="text-rose-700">{model.accessErrorMessage}</AlertDescription>
              </Alert>
            ) : null}
            {model.accessSuccessMessage ? (
              <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
                <AlertDescription className="text-emerald-700">{model.accessSuccessMessage}</AlertDescription>
              </Alert>
            ) : null}
          </div>
        </DialogContent>
      </Dialog>
    </main>
  )
}
