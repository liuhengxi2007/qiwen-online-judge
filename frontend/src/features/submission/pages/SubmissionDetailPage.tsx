import { Link, Navigate, useParams } from 'react-router-dom'
import { ArrowLeft, Files, LogOut } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import {
  parseSubmissionId,
  submissionIdValue,
  submissionLanguageLabel,
  submissionStatusLabel,
  submissionVerdictLabel,
  submissionSourceCodeValue,
} from '@/features/submission/domain/submission'
import { useSubmissionDetailQuery } from '@/features/submission/hooks/use-submission-detail-query'
import { usePageTitle } from '@/shared/hooks/use-page-title'

export function SubmissionDetailPage() {
  usePageTitle('Qiwen Online Judge - Submission Detail')
  const { session: user, signOut, navigationIntent } = useSessionGuard()
  const { submissionId } = useParams<{ submissionId: string }>()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const parsedSubmissionId = Number(submissionId ?? '')
  const submissionIdResult = parseSubmissionId(parsedSubmissionId)
  if (!submissionIdResult.ok) {
    return <Navigate replace to="/submissions" />
  }

  const submissionQuery = useSubmissionDetailQuery(submissionIdResult.value)

  if (!submissionQuery.isLoading && !submissionQuery.submission) {
    return <Navigate replace to="/submissions" />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#edf4fb_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-5xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Qiwen Online Judge</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">
              Submission Detail
            </h1>
            <p className="text-sm text-slate-600">
              Signed in as {displayNameValue(user.displayName)} ({usernameValue(user.username)}).
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Button asChild variant="outline" className="rounded-full border-slate-300 bg-white">
              <Link to="/submissions">
                <ArrowLeft className="size-4" />
                Back to Submissions
              </Link>
            </Button>
            <Button
              type="button"
              variant="outline"
              className="rounded-full border-slate-300 bg-white"
              onClick={() => {
                void signOut()
              }}
            >
              <LogOut className="size-4" />
              Sign out
            </Button>
          </div>
        </div>

        {submissionQuery.errorMessage ? (
          <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{submissionQuery.errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {submissionQuery.isLoading ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">Loading submission details...</CardContent>
          </Card>
        ) : submissionQuery.submission ? (
          <div className="space-y-6">
            <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
              <CardHeader>
                <div className="flex items-center gap-3">
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
                    <Files className="size-5" />
                  </div>
                  <div>
                    <CardTitle className="text-2xl text-slate-950">
                      Submission {submissionIdValue(submissionQuery.submission.id)}
                    </CardTitle>
                    <CardDescription className="mt-2 text-sm text-slate-500">
                      Problem {submissionQuery.submission.problemSlug}
                    </CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="grid gap-4 text-sm text-slate-600 sm:grid-cols-2 lg:grid-cols-6">
                <div>
                  <p className="text-slate-500">Submitter</p>
                  <p className="mt-1 font-medium text-slate-900">{submissionQuery.submission.submitterUsername}</p>
                </div>
                <div>
                  <p className="text-slate-500">Language</p>
                  <p className="mt-1 font-medium text-slate-900">
                    {submissionLanguageLabel(submissionQuery.submission.language)}
                  </p>
                </div>
                <div>
                  <p className="text-slate-500">Status</p>
                  <p className="mt-1 font-medium text-slate-900">
                    {submissionStatusLabel(submissionQuery.submission.status)}
                  </p>
                </div>
                <div>
                  <p className="text-slate-500">Verdict</p>
                  <p className="mt-1 font-medium text-slate-900">
                    {submissionVerdictLabel(submissionQuery.submission.verdict)}
                  </p>
                </div>
                <div>
                  <p className="text-slate-500">Submitted at</p>
                  <p className="mt-1 font-medium text-slate-900">
                    {new Date(submissionQuery.submission.submittedAt).toLocaleString()}
                  </p>
                </div>
                <div>
                  <p className="text-slate-500">Problem slug</p>
                  <p className="mt-1 font-medium text-slate-900">{submissionQuery.submission.problemSlug}</p>
                </div>
              </CardContent>
            </Card>

            {submissionQuery.submission.judgeMessage ? (
              <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
                <CardHeader>
                  <CardTitle className="text-xl text-slate-950">Judge message</CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="whitespace-pre-wrap text-sm leading-7 text-slate-700">
                    {submissionQuery.submission.judgeMessage}
                  </p>
                </CardContent>
              </Card>
            ) : null}

            <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
              <CardHeader>
                <CardTitle className="text-xl text-slate-950">Source code</CardTitle>
                <CardDescription>This page shows the exact source code saved in the submission record.</CardDescription>
              </CardHeader>
              <CardContent>
                <pre className="overflow-x-auto rounded-3xl bg-slate-950 p-6 text-sm leading-7 text-slate-100">
                  <code>{submissionSourceCodeValue(submissionQuery.submission.sourceCode)}</code>
                </pre>
              </CardContent>
            </Card>
          </div>
        ) : null}
      </section>
    </main>
  )
}
