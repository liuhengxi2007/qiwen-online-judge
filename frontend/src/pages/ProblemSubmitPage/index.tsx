import { useState } from 'react'
import { Navigate, useParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { ProblemSubmitEditorCard } from './components/ProblemSubmitEditorCard'
import {
  buildSubmissionPayload,
  hasSubmissionDraftSource,
  normalizeSubmitProgramDraft,
  type SubmitProgramDraft,
} from './functions/SubmitPrograms'
import { ProblemSubmitHeaderCard } from './components/ProblemSubmitHeaderCard'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { parseContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { useProblemSubmitDetailQuery } from './hooks/useProblemSubmitDetailQuery'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import { useCreateSubmissionAction } from './hooks/useCreateSubmissionAction'
import { PageLoadingCard } from '@/pages/components/PageLoadingCard'
import { PageShell } from '@/pages/components/PageShell'
import { useBeforeUnloadPrompt } from '@/pages/hooks/useBeforeUnloadPrompt'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'

const supportedLanguages: Array<{ value: SubmissionLanguage; label: string }> = [
  { value: 'cpp17', label: 'C++17' },
  { value: 'python3', label: 'Python 3' },
  { value: 'text', label: 'Text' },
]

/**
 * 题目提交页入口，校验普通或比赛内题目路由参数后进入提交表单。
 */
export function ProblemSubmitPage() {
  const { t } = useI18n()
  usePageTitle(t('problem.submit.pageTitle'))
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

  return <ProblemSubmitPageContent contestSlug={parsedContestSlug} problemSlug={slugResult.value} />
}

/**
 * 题目提交页主体，加载题目详情并组合提交头部、编辑器和创建提交动作。
 */
function ProblemSubmitPageContent({ contestSlug, problemSlug }: { contestSlug?: ContestSlug; problemSlug: ProblemSlug }) {
  const { t } = useI18n()
  const detailQuery = useProblemSubmitDetailQuery(problemSlug, contestSlug)
  const [programs, setPrograms] = useState<SubmitProgramDraft[]>([
    { id: 'main', role: 'main', language: 'cpp17', sourceMode: 'paste', sourceCode: '', sourceFile: null },
  ])
  const createSubmissionAction = useCreateSubmissionAction(t('problem.submit.createFailed'), contestSlug)
  const hasUnsavedChanges = programs.some(hasSubmissionDraftSource)
  const canSubmit = programs.length > 0 && programs.every(hasSubmissionDraftSource)

  useBeforeUnloadPrompt(hasUnsavedChanges)

  if (!detailQuery.isLoading && !detailQuery.problem) {
    return <Navigate replace to="/problems" />
  }

  return (
    <PageShell title={t('problem.submit.heading')} mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#edf5f1_100%)]">
      {detailQuery.errorMessage ? (
        <Alert variant="destructive" className="mb-6">
          <AlertDescription>{detailQuery.errorMessage}</AlertDescription>
        </Alert>
      ) : null}

      {detailQuery.isLoading ? (
        <PageLoadingCard message={t('problem.submit.loading')} />
      ) : detailQuery.problem ? (
        <div className="space-y-6">
          <ProblemSubmitHeaderCard detailQuery={detailQuery} />
          <ProblemSubmitEditorCard
            errorMessage={createSubmissionAction.errorMessage}
            isSubmitting={createSubmissionAction.isSubmitting}
            onAddProgram={() => {
              setPrograms((currentPrograms) => [
                ...currentPrograms,
                {
                  id: crypto.randomUUID(),
                  role: `role-${currentPrograms.length + 1}`,
                  language: 'cpp17',
                  sourceMode: 'paste',
                  sourceCode: '',
                  sourceFile: null,
                },
              ])
              createSubmissionAction.clearMessages()
            }}
            onProgramChange={(id, update) => {
              setPrograms((currentPrograms) =>
                currentPrograms.map((program) => (program.id === id ? normalizeSubmitProgramDraft(program, update) : program)),
              )
              createSubmissionAction.clearMessages()
            }}
            onRemoveProgram={(id) => {
              setPrograms((currentPrograms) => currentPrograms.filter((program) => program.id !== id))
              createSubmissionAction.clearMessages()
            }}
            onSubmit={() => {
              const payload = buildSubmissionPayload(problemSlug, programs)
              if (!payload.ok) {
                createSubmissionAction.setErrorMessage(payload.error)
                return
              }

              void createSubmissionAction.submit(payload.payload)
            }}
            canSubmit={canSubmit}
            programs={programs}
            statusMessage={createSubmissionAction.statusMessage}
            supportedLanguages={supportedLanguages}
          />
        </div>
      ) : null}
    </PageShell>
  )
}
