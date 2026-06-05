import { useState } from 'react'
import { Navigate, useParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { ProblemSubmitEditorCard, type SubmitProgramDraft } from './components/ProblemSubmitEditorCard'
import { ProblemSubmitHeaderCard } from './components/ProblemSubmitHeaderCard'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { parseContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { useProblemDetailQuery } from '@/pages/hooks/useProblemDetailQuery'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import { useCreateSubmissionAction } from './hooks/useCreateSubmissionAction'
import { parseSubmissionSourceCode } from '@/objects/submission/SubmissionSourceCode'
import { PageLoadingCard } from '@/pages/components/PageLoadingCard'
import { PageShell } from '@/pages/components/PageShell'
import { useBeforeUnloadPrompt } from '@/pages/hooks/useBeforeUnloadPrompt'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'
import type { CreateSubmissionRequest } from '@/objects/submission/request/CreateSubmissionRequest'

const supportedLanguages: Array<{ value: SubmissionLanguage; label: string }> = [
  { value: 'cpp17', label: 'C++17' },
  { value: 'python3', label: 'Python 3' },
]

const rolePattern = /^[A-Za-z0-9_-]+$/

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

function ProblemSubmitPageContent({ contestSlug, problemSlug }: { contestSlug?: ContestSlug; problemSlug: ProblemSlug }) {
  const { t } = useI18n()
  const detailQuery = useProblemDetailQuery(problemSlug, contestSlug)
  const [programs, setPrograms] = useState<SubmitProgramDraft[]>([
    { id: 'main', role: 'main', language: 'cpp17', sourceCode: '' },
  ])
  const createSubmissionAction = useCreateSubmissionAction(t('problem.submit.createFailed'), contestSlug)
  const hasUnsavedChanges = programs.some((program) => program.sourceCode.trim().length > 0)

  useBeforeUnloadPrompt(hasUnsavedChanges)

  if (!detailQuery.isLoading && !detailQuery.problem) {
    return <Navigate replace to="/problems" />
  }

  return (
    <PageShell title={t('problem.submit.heading')} mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#edf5f1_100%)]">
      {detailQuery.errorMessage ? (
        <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{detailQuery.errorMessage}</AlertDescription>
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
                  sourceCode: '',
                },
              ])
              createSubmissionAction.clearMessages()
            }}
            onProgramChange={(id, update) => {
              setPrograms((currentPrograms) =>
                currentPrograms.map((program) => (program.id === id ? { ...program, ...update } : program)),
              )
              createSubmissionAction.clearMessages()
            }}
            onRemoveProgram={(id) => {
              setPrograms((currentPrograms) => currentPrograms.filter((program) => program.id !== id))
              createSubmissionAction.clearMessages()
            }}
            onSubmit={() => {
              const payloadPrograms: CreateSubmissionRequest['programs'] = {}
              const seenRoles = new Set<string>()

              for (const program of programs) {
                const role = program.role.trim()
                if (!role || !rolePattern.test(role)) {
                  createSubmissionAction.setErrorMessage('Role must contain only ASCII letters, digits, "_" or "-".')
                  return
                }
                if (seenRoles.has(role)) {
                  createSubmissionAction.setErrorMessage(`Role is duplicated: ${role}.`)
                  return
                }
                seenRoles.add(role)

                const sourceCodeResult = parseSubmissionSourceCode(program.sourceCode)
                if (!sourceCodeResult.ok) {
                  createSubmissionAction.setErrorMessage(sourceCodeResult.error)
                  return
                }
                payloadPrograms[role] = {
                  language: program.language,
                  sourceCode: sourceCodeResult.value,
                }
              }

              if (Object.keys(payloadPrograms).length === 0) {
                createSubmissionAction.setErrorMessage('At least one program is required.')
                return
              }

              void createSubmissionAction.submit({
                problemSlug,
                programs: payloadPrograms,
              })
            }}
            programs={programs}
            statusMessage={createSubmissionAction.statusMessage}
            supportedLanguages={supportedLanguages}
          />
        </div>
      ) : null}
    </PageShell>
  )
}
