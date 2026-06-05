import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useNavigate } from 'react-router-dom'
import { FilePlus2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { useCreateProblemPageModel } from './hooks/useCreateProblemPageModel'
import { MarkdownEditorTabs } from '@/pages/components/MarkdownEditorTabs'
import { PageShell } from '@/pages/components/PageShell'
import { ResourceAccessEditor } from '@/pages/components/ResourceAccessEditor'
import { useBeforeUnloadPrompt } from '@/pages/hooks/useBeforeUnloadPrompt'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
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

  return <CreateProblemPageContent canCreate={user.problemManager} />
}

function CreateProblemPageContent({ canCreate }: { canCreate: boolean }) {
  const { t } = useI18n()
  const otherUserSubmissionAccessOptions = [
    { value: 'none', label: t('problem.otherUserSubmissionAccess.none.label'), description: t('problem.otherUserSubmissionAccess.none.description') },
    { value: 'summary', label: t('problem.otherUserSubmissionAccess.summary.label'), description: t('problem.otherUserSubmissionAccess.summary.description') },
    { value: 'detail', label: t('problem.otherUserSubmissionAccess.detail.label'), description: t('problem.otherUserSubmissionAccess.detail.description') },
  ] as const
  const navigate = useNavigate()
  const model = useCreateProblemPageModel(canCreate)
  const [statementTab, setStatementTab] = useState<'write' | 'preview'>('write')
  const hasUnsavedChanges =
    model.slug.trim().length > 0 ||
    model.title.trim().length > 0 ||
    model.statement.trim().length > 0 ||
    model.baseAccess !== 'restricted' ||
    model.grantedUsersInput.trim().length > 0 ||
    model.grantedGroupsInput.trim().length > 0 ||
    model.managerUsersInput.trim().length > 0 ||
    model.managerGroupsInput.trim().length > 0 ||
    model.otherUserSubmissionAccess !== 'none'

  useBeforeUnloadPrompt(hasUnsavedChanges)

  return (
    <PageShell
      title={t('problem.create.heading')}
      mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#edf5f1_100%)]"
    >
      <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
              <FilePlus2 className="size-5" />
            </div>
            <div>
              <CardTitle className="text-xl text-slate-950">{t('problem.create.cardTitle')}</CardTitle>
              <CardDescription>{t('problem.create.cardDescription')}</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-5">
          {!canCreate ? (
            <Alert className="rounded-2xl border-amber-200 bg-amber-50/95">
              <AlertDescription className="text-amber-900">{t('problem.create.permissionRequired')}</AlertDescription>
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
            <Input id="problem-title" value={model.title} onChange={(event) => model.setTitle(event.target.value)} />
          </div>

          <div className="space-y-2">
            <Label htmlFor="problem-statement">{t('problem.create.statement')}</Label>
            <MarkdownEditorTabs
              textareaId="problem-statement"
              value={model.statement}
              tab={statementTab}
              onTabChange={setStatementTab}
              onValueChange={model.setStatement}
              textareaClassName="min-h-64 !font-mono"
            />
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
            <Label htmlFor="problem-other-user-submission-access">{t('problem.create.otherUserSubmissionAccess')}</Label>
            <Select value={model.otherUserSubmissionAccess} onValueChange={model.setOtherUserSubmissionAccess}>
              <SelectTrigger id="problem-other-user-submission-access" className="rounded-2xl">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {otherUserSubmissionAccessOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <p className="text-xs text-slate-500">
              {otherUserSubmissionAccessOptions.find((option) => option.value === model.otherUserSubmissionAccess)?.description}
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
    </PageShell>
  )
}
