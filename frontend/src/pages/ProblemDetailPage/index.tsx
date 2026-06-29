import { useState } from 'react'
import { Navigate, useParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import { parseContestSlug } from '@/objects/contest/ContestSlug'
import { problemStatementTextValue } from '@/objects/problem/ProblemStatementText'
import { problemTitleValue } from '@/objects/problem/ProblemTitle'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { ProblemAccessDialog } from './components/ProblemAccessDialog'
import { ProblemDetailHeaderCard } from './components/ProblemDetailHeaderCard'
import { ProblemEditDialog } from './components/ProblemEditDialog'
import { useProblemDetailPageModel } from './hooks/useProblemDetailPageModel'
import { PageLoadingCard } from '@/pages/components/PageLoadingCard'
import { PageShell } from '@/pages/components/PageShell'
import {
  grantedGroupsInputFromAccessPolicy,
  grantedManagerGroupsInputFromAccessPolicy,
  grantedManagerUsersInputFromAccessPolicy,
  grantedUsersInputFromAccessPolicy,
  normalizeAccessSubjectInput,
} from '@/pages/components/ResourceAccessEditorInput'
import { useBeforeUnloadPrompt } from '@/pages/hooks/useBeforeUnloadPrompt'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'
import { usernameValue } from '@/objects/user/Username'

/**
 * 题目详情页入口，解析普通或比赛内题目路由参数后进入详情内容。
 */
export function ProblemDetailPage() {
  const { t } = useI18n()
  usePageTitle(t('problem.detail.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const { contestSlug, slug } = useParams<{ contestSlug?: string; slug: string }>()

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

  const contestSlugResult = contestSlug ? parseContestSlug(contestSlug) : null
  if (contestSlugResult && !contestSlugResult.ok) {
    return <Navigate replace to="/contests" />
  }

  const parsedContestSlug = contestSlugResult?.ok ? contestSlugResult.value : undefined

  return <ProblemDetailPageContent contestSlug={parsedContestSlug} problemSlug={slugResult.value} />
}

/**
 * 题目详情页主体，组合会话守卫、题目查询、编辑器和删除/更新动作。
 */
function ProblemDetailPageContent({ contestSlug, problemSlug }: { contestSlug?: ContestSlug; problemSlug: ProblemSlug }) {
  const { t } = useI18n()
  const otherUserSubmissionAccessOptions = [
    { value: 'none', label: t('problem.otherUserSubmissionAccess.none.label'), description: t('problem.otherUserSubmissionAccess.none.description') },
    { value: 'summary', label: t('problem.otherUserSubmissionAccess.summary.label'), description: t('problem.otherUserSubmissionAccess.summary.description') },
    { value: 'detail', label: t('problem.otherUserSubmissionAccess.detail.label'), description: t('problem.otherUserSubmissionAccess.detail.description') },
  ] as const
  const model = useProblemDetailPageModel(problemSlug, contestSlug)
  const canManageProblem = model.canManage
  const [managementPanel, setManagementPanel] = useState<'edit' | 'access' | null>(null)
  const [statementTab, setStatementTab] = useState<'write' | 'preview'>('write')
  const hasUnsavedChanges =
    model.problem !== null &&
    (model.title !== problemTitleValue(model.problem.title) ||
      model.statement !== problemStatementTextValue(model.problem.statement) ||
      model.authorUsername.trim() !== (model.problem.author ? usernameValue(model.problem.author.username) : '') ||
      model.baseAccess !== model.problem.accessPolicy.baseAccess ||
      normalizeAccessSubjectInput(model.grantedUsersInput) !==
        grantedUsersInputFromAccessPolicy(model.problem.accessPolicy) ||
      normalizeAccessSubjectInput(model.grantedGroupsInput) !==
        grantedGroupsInputFromAccessPolicy(model.problem.accessPolicy) ||
      normalizeAccessSubjectInput(model.managerUsersInput) !==
        grantedManagerUsersInputFromAccessPolicy(model.problem.accessPolicy) ||
      normalizeAccessSubjectInput(model.managerGroupsInput) !==
        grantedManagerGroupsInputFromAccessPolicy(model.problem.accessPolicy) ||
      model.otherUserSubmissionAccess !== model.problem.otherUserSubmissionAccess)

  useBeforeUnloadPrompt(hasUnsavedChanges)

  return (
    <>
      <PageShell title={t('problem.detail.heading')} mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#edf5f1_100%)]">
        {!model.isLoading && !model.problem && model.loadErrorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{model.loadErrorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {model.isLoading ? (
          <PageLoadingCard message={t('problem.detail.loading')} />
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
      </PageShell>

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
        otherUserSubmissionAccessOptions={otherUserSubmissionAccessOptions}
        setOpen={(open) => {
          setManagementPanel(open ? 'access' : null)
        }}
      />
    </>
  )
}
