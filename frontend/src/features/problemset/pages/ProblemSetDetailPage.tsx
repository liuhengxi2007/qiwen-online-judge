import { useState } from 'react'
import { Navigate, useParams } from 'react-router-dom'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent } from '@/components/ui/card'
import { EditProblemSetDialog } from '@/features/problemset/components/edit-problem-set-dialog'
import { ProblemSetAccessDialog } from '@/features/problemset/components/problem-set-access-dialog'
import { ProblemSetDetailHeaderCard } from '@/features/problemset/components/problem-set-detail-header-card'
import { ProblemSetLinkedProblemsCard } from '@/features/problemset/components/problem-set-linked-problems-card'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { parseProblemSetSlug, problemSetDescriptionValue, problemSetTitleValue, type ProblemSetSlug } from '@/features/problemset/domain/problemset'
import { useProblemSetDetailPageModel } from '@/features/problemset/hooks/use-problemset-detail-page-model'
import {
  grantedGroupsInputFromAccessPolicy,
  grantedUsersInputFromAccessPolicy,
  normalizeAccessSubjectInput,
} from '@/shared/domain/resource-access-input'
import { AppSectionBar } from '@/shared/components/app-section-bar'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { useBeforeUnloadPrompt } from '@/shared/hooks/use-before-unload-prompt'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/use-i18n'

export function ProblemSetDetailPage() {
  const { t } = useI18n()
  usePageTitle(t('problemSet.detail.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const { slug } = useParams<{ slug: string }>()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const slugResult = parseProblemSetSlug(slug ?? '')
  if (!slugResult.ok) {
    return <Navigate replace to="/problem-sets" />
  }

  return <ProblemSetDetailPageContent canManageProblems={user.siteManager || user.problemManager} problemSetSlug={slugResult.value} />
}

function ProblemSetDetailPageContent({
  canManageProblems,
  problemSetSlug,
}: {
  canManageProblems: boolean
  problemSetSlug: ProblemSetSlug
}) {
  const { t } = useI18n()
  const model = useProblemSetDetailPageModel(problemSetSlug, canManageProblems)
  const [managementPanel, setManagementPanel] = useState<'edit' | 'access' | null>(null)
  const hasUnsavedChanges =
    model.problemSet !== null &&
    (model.title !== problemSetTitleValue(model.problemSet.title) ||
      model.description !== problemSetDescriptionValue(model.problemSet.description) ||
      model.baseAccess !== model.problemSet.accessPolicy.baseAccess ||
      normalizeAccessSubjectInput(model.grantedUsersInput) !==
        grantedUsersInputFromAccessPolicy(model.problemSet.accessPolicy) ||
      normalizeAccessSubjectInput(model.grantedGroupsInput) !==
        grantedGroupsInputFromAccessPolicy(model.problemSet.accessPolicy) ||
      model.linkProblemSlug.trim().length > 0)

  useBeforeUnloadPrompt(hasUnsavedChanges)

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#fdf8fb_0%,#f4edf7_48%,#ecf3fb_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('problemSet.detail.heading')}</h1>
          </div>

          <AncestorNavigation />
        </div>

        <AppSectionBar />

        {!model.isLoading && !model.problemSet && model.loadErrorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{model.loadErrorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {model.isLoading ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">{t('problemSet.detail.loading')}</CardContent>
          </Card>
        ) : model.problemSet ? (
          <div className="space-y-6">
            <ProblemSetDetailHeaderCard
              problemSet={model.problemSet}
              canManageProblems={canManageProblems}
              managementPanel={managementPanel}
              onTogglePanel={(panel) => {
                setManagementPanel((currentPanel) => (currentPanel === panel ? null : panel))
              }}
            />
            <ProblemSetLinkedProblemsCard
              problems={model.problemSet.problems}
              canManageProblems={canManageProblems}
              activeRemovingProblemSlug={model.activeRemovingProblemSlug}
              errorMessage={model.problemListErrorMessage}
              successMessage={model.problemListSuccessMessage}
              onRemoveProblem={(problemSlug) => {
                void model.removeProblem(problemSlug)
              }}
            />
          </div>
        ) : null}
      </section>

      <EditProblemSetDialog
        open={canManageProblems && managementPanel === 'edit'}
        title={model.title}
        description={model.description}
        linkProblemSlug={model.linkProblemSlug}
        isSaving={model.isSaving}
        isDeleting={model.isDeleting}
        activeLink={model.activeLink}
        contentErrorMessage={model.contentErrorMessage}
        contentSuccessMessage={model.contentSuccessMessage}
        linkErrorMessage={model.linkErrorMessage}
        linkSuccessMessage={model.linkSuccessMessage}
        onOpenChange={(open) => {
          setManagementPanel(open ? 'edit' : null)
        }}
        onTitleChange={model.setTitle}
        onDescriptionChange={model.setDescription}
        onLinkProblemSlugChange={model.setLinkProblemSlug}
        onSaveContent={() => {
          void model.saveContent()
        }}
        onAttachProblem={() => {
          void model.attachProblem()
        }}
        onDeleteProblemSet={model.deleteCurrentProblemSet}
      />
      <ProblemSetAccessDialog
        open={canManageProblems && managementPanel === 'access'}
        accessPolicy={model.accessPolicy}
        summaryPolicy={model.problemSet?.accessPolicy ?? model.accessPolicy}
        grantedUsersInput={model.grantedUsersInput}
        grantedGroupsInput={model.grantedGroupsInput}
        isSaving={model.isSaving}
        errorMessage={model.accessErrorMessage}
        successMessage={model.accessSuccessMessage}
        onOpenChange={(open) => {
          setManagementPanel(open ? 'access' : null)
        }}
        onBaseAccessChange={model.setBaseAccess}
        onGrantedUsersInputChange={model.setGrantedUsersInput}
        onGrantedGroupsInputChange={model.setGrantedGroupsInput}
        onSave={() => {
          void model.saveAccess()
        }}
      />
    </main>
  )
}
