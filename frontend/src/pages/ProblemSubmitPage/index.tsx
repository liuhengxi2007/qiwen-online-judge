import { useState } from 'react'
import { Navigate, useParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { ProblemSubmitEditorCard } from './components/ProblemSubmitEditorCard'
import { ProblemSubmitHeaderCard } from './components/ProblemSubmitHeaderCard'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { useProblemDetailQuery } from '@/pages/hooks/useProblemDetailQuery'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import { useCreateSubmissionAction } from './hooks/useCreateSubmissionAction'
import { parseSubmissionSourceCode } from '@/objects/submission/SubmissionSourceCode'
import { PageLoadingCard } from '@/pages/components/PageLoadingCard'
import { PageShell } from '@/pages/components/PageShell'
import { useBeforeUnloadPrompt } from '@/pages/hooks/useBeforeUnloadPrompt'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'

const supportedLanguages: Array<{ value: SubmissionLanguage; label: string }> = [
  { value: 'cpp17', label: 'C++17' },
  { value: 'python3', label: 'Python 3' },
]

export function ProblemSubmitPage() {
  const { t } = useI18n()
  usePageTitle(t('problem.submit.pageTitle'))
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

  return <ProblemSubmitPageContent problemSlug={slugResult.value} />
}

function ProblemSubmitPageContent({ problemSlug }: { problemSlug: ProblemSlug }) {
  const { t } = useI18n()
  const detailQuery = useProblemDetailQuery(problemSlug)
  const [language, setLanguage] = useState<SubmissionLanguage>('cpp17')
  const [sourceCode, setSourceCode] = useState('')
  const createSubmissionAction = useCreateSubmissionAction(t('problem.submit.createFailed'))
  const hasUnsavedChanges = sourceCode.trim().length > 0

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
            language={language}
            onLanguageChange={(nextLanguage) => {
              setLanguage(nextLanguage)
              createSubmissionAction.clearMessages()
            }}
            onSourceCodeChange={(value) => {
              setSourceCode(value)
              createSubmissionAction.clearMessages()
            }}
            onSubmit={() => {
              const sourceCodeResult = parseSubmissionSourceCode(sourceCode)
              if (!sourceCodeResult.ok) {
                createSubmissionAction.setErrorMessage(sourceCodeResult.error)
                return
              }

              void createSubmissionAction.submit({
                problemSlug,
                language,
                sourceCode: sourceCodeResult.value,
              })
            }}
            sourceCode={sourceCode}
            statusMessage={createSubmissionAction.statusMessage}
            supportedLanguages={supportedLanguages}
          />
        </div>
      ) : null}
    </PageShell>
  )
}
