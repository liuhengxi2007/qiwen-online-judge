import { useState } from 'react'
import { Navigate, useParams } from 'react-router-dom'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { EditProblemSetDialog } from './components/EditProblemSetDialog'
import { ProblemSetAccessDialog } from './components/ProblemSetAccessDialog'
import { ProblemSetDetailHeaderCard } from './components/ProblemSetDetailHeaderCard'
import { ProblemSetLinkedProblemsCard } from './components/ProblemSetLinkedProblemsCard'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { problemSetDescriptionValue } from '@/objects/problemset/ProblemSetDescription'
import { parseProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { problemSetTitleValue } from '@/objects/problemset/ProblemSetTitle'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { useProblemSetDetailPageModel } from './hooks/useProblemSetDetailPageModel'
import {
  grantedGroupsInputFromAccessPolicy,
  grantedUsersInputFromAccessPolicy,
  normalizeAccessSubjectInput,
} from '@/pages/components/ResourceAccessEditorInput'
import { PageLoadingCard } from '@/pages/components/PageLoadingCard'
import { PageShell } from '@/pages/components/PageShell'
import { useBeforeUnloadPrompt } from '@/pages/hooks/useBeforeUnloadPrompt'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'
import { usernameValue } from '@/objects/user/Username'

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

  return <ProblemSetDetailPageContent canManageProblems={user.problemManager} problemSetSlug={slugResult.value} />
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
      model.authorUsername.trim() !== (model.problemSet.author ? usernameValue(model.problemSet.author.username) : '') ||
      model.baseAccess !== model.problemSet.accessPolicy.baseAccess ||
      normalizeAccessSubjectInput(model.grantedUsersInput) !==
        grantedUsersInputFromAccessPolicy(model.problemSet.accessPolicy) ||
      normalizeAccessSubjectInput(model.grantedGroupsInput) !==
        grantedGroupsInputFromAccessPolicy(model.problemSet.accessPolicy) ||
      model.linkProblemSlug.trim().length > 0)

  useBeforeUnloadPrompt(hasUnsavedChanges)

  return (
    <>
      <PageShell
        title={t('problemSet.detail.heading')}
        mainClassName="bg-[linear-gradient(180deg,#fdf8fb_0%,#f4edf7_48%,#ecf3fb_100%)]"
      >
        {!model.isLoading && !model.problemSet && model.loadErrorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{model.loadErrorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {model.isLoading ? (
          <PageLoadingCard message={t('problemSet.detail.loading')} />
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
      </PageShell>

      <EditProblemSetDialog
        open={canManageProblems && managementPanel === 'edit'}
        title={model.title}
        description={model.description}
        authorUsername={model.authorUsername}
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
        onAuthorUsernameChange={model.setAuthorUsername}
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
    </>
  )
}
