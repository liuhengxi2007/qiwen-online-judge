import { useState } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent } from '@/components/ui/card'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { ProblemSubmitEditorCard } from '@/features/problem/components/problem-submit-editor-card'
import { ProblemSubmitHeaderCard } from '@/features/problem/components/problem-submit-header-card'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import {
  parseProblemSlug,
} from '@/features/problem/domain/problem'
import { useProblemDetailQuery } from '@/features/problem/hooks/use-problem-detail-query'
import { createSubmission } from '@/features/submission/api/submission-client'
import {
  type SubmissionLanguage,
  parseSubmissionSourceCode,
  submissionIdValue,
} from '@/features/submission/domain/submission'
import { HttpClientError } from '@/shared/api/http-client'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { useBeforeUnloadPrompt } from '@/shared/hooks/use-before-unload-prompt'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/i18n'

const supportedLanguages: Array<{ value: SubmissionLanguage; label: string }> = [
  { value: 'cpp17', label: 'C++17' },
  { value: 'python3', label: 'Python 3' },
]

export function ProblemSubmitPage() {
  const { t } = useI18n()
  usePageTitle(t('problem.submit.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const { slug } = useParams<{ slug: string }>()
  const navigate = useNavigate()

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

  const detailQuery = useProblemDetailQuery(slugResult.value)
  const [language, setLanguage] = useState<SubmissionLanguage>('cpp17')
  const [sourceCode, setSourceCode] = useState('')
  const [statusMessage, setStatusMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const hasUnsavedChanges = sourceCode.trim().length > 0

  useBeforeUnloadPrompt(hasUnsavedChanges)

  if (!detailQuery.isLoading && !detailQuery.problem) {
    return <Navigate replace to="/problems" />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#edf5f1_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('problem.submit.heading')}</h1>
            <p className="text-sm text-slate-600">
              {t('common.signedInAs', { displayName: displayNameValue(user.displayName), username: usernameValue(user.username) })}
            </p>
          </div>

          <AncestorNavigation />
        </div>

        {detailQuery.errorMessage ? (
          <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{detailQuery.errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {detailQuery.isLoading ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">{t('problem.submit.loading')}</CardContent>
          </Card>
        ) : detailQuery.problem ? (
          <div className="space-y-6">
            <ProblemSubmitHeaderCard detailQuery={detailQuery} />
            <ProblemSubmitEditorCard
              errorMessage={errorMessage}
              isSubmitting={isSubmitting}
              language={language}
              onLanguageChange={(nextLanguage) => {
                setLanguage(nextLanguage)
                setStatusMessage('')
                setErrorMessage('')
              }}
              onSourceCodeChange={(value) => {
                setSourceCode(value)
                setStatusMessage('')
                setErrorMessage('')
              }}
              onSubmit={() => {
                const sourceCodeResult = parseSubmissionSourceCode(sourceCode)
                if (!sourceCodeResult.ok) {
                  setErrorMessage(sourceCodeResult.error)
                  setStatusMessage('')
                  return
                }

                setIsSubmitting(true)
                setErrorMessage('')
                setStatusMessage('')

                void createSubmission({
                  problemSlug: slugResult.value,
                  language,
                  sourceCode: sourceCodeResult.value,
                })
                  .then((submission) => {
                    void navigate(`/submissions/${submissionIdValue(submission.id)}`)
                  })
                  .catch((error: unknown) => {
                    if (error instanceof HttpClientError) {
                      setErrorMessage(error.message)
                      return
                    }

                    setErrorMessage(t('problem.submit.createFailed'))
                  })
                  .finally(() => {
                    setIsSubmitting(false)
                  })
              }}
              sourceCode={sourceCode}
              statusMessage={statusMessage}
              supportedLanguages={supportedLanguages}
            />
          </div>
        ) : null}
      </section>
    </main>
  )
}
