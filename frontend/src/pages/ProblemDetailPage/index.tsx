import { useState } from 'react'
import { Navigate, useParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent } from '@/components/ui/card'
import { useSessionGuard } from '@/pages/hooks/use-session-guard'
import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemStatementTextValue } from '@/objects/problem/ProblemStatementText'
import { problemTitleValue } from '@/objects/problem/ProblemTitle'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { ProblemAccessDialog } from './components/problem-access-dialog'
import { ProblemDetailHeaderCard } from './components/problem-detail-header-card'
import { ProblemEditDialog } from './components/problem-edit-dialog'
import { useProblemDetailPageModel } from './hooks/use-problem-detail-page-model'
import { AppSectionBar } from '@/pages/components/app-section-bar'
import { AncestorNavigation } from '@/pages/components/ancestor-navigation'
import {
  grantedGroupsInputFromAccessPolicy,
  grantedManagerGroupsInputFromAccessPolicy,
  grantedManagerUsersInputFromAccessPolicy,
  grantedUsersInputFromAccessPolicy,
  normalizeAccessSubjectInput,
} from '@/pages/components/resource-access-editor-input'
import { useBeforeUnloadPrompt } from '@/pages/hooks/use-before-unload-prompt'
import { usePageTitle } from '@/pages/hooks/use-page-title'
import { useI18n } from '@/system/i18n/use-i18n'

export function ProblemDetailPage() {
  const { t } = useI18n()
  usePageTitle(t('problem.detail.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
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

  return <ProblemDetailPageContent problemSlug={slugResult.value} />
}

function ProblemDetailPageContent({ problemSlug }: { problemSlug: ProblemSlug }) {
  const { t } = useI18n()
  const othersSubmissionAccessOptions = [
    { value: 'none', label: t('problem.others.none.label'), description: t('problem.others.none.description') },
    { value: 'summary', label: t('problem.others.summary.label'), description: t('problem.others.summary.description') },
    { value: 'detail', label: t('problem.others.detail.label'), description: t('problem.others.detail.description') },
  ] as const
  const model = useProblemDetailPageModel(problemSlug)
  const canManageProblem = model.canManage
  const [managementPanel, setManagementPanel] = useState<'edit' | 'access' | null>(null)
  const [statementTab, setStatementTab] = useState<'write' | 'preview'>('write')
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
          </div>

          <AncestorNavigation />
        </div>

        <AppSectionBar />

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
            <ProblemDetailHeaderCard
              canManageProblem={canManageProblem}
              managementPanel={managementPanel}
              model={model}
              setManagementPanel={setManagementPanel}
            />
          </div>
        ) : null}
      </section>

      <ProblemEditDialog
        model={model}
        open={canManageProblem && managementPanel === 'edit'}
        setOpen={(open) => {
          setManagementPanel(open ? 'edit' : null)
        }}
        statementTab={statementTab}
        setStatementTab={setStatementTab}
      />
      <ProblemAccessDialog
        model={model}
        open={canManageProblem && managementPanel === 'access'}
        othersSubmissionAccessOptions={othersSubmissionAccessOptions}
        setOpen={(open) => {
          setManagementPanel(open ? 'access' : null)
        }}
      />
    </main>
  )
}
