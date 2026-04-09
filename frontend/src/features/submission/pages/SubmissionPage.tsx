import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { ArrowRight, Files } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  displayNameValue,
  parseUsername,
  usernameValue,
  type Username,
} from '@/features/auth/domain/auth'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import {
  submissionIdValue,
  submissionLanguageLabel,
  submissionStatusLabel,
  submissionVerdictLabel,
} from '@/features/submission/domain/submission'
import { useSubmissionListQuery } from '@/features/submission/hooks/use-submission-list-query'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { usePageTitle } from '@/shared/hooks/use-page-title'

export function SubmissionPage() {
  usePageTitle('Qiwen Online Judge - Submissions')
  const { session: user, navigationIntent } = useSessionGuard()
  const [filterInput, setFilterInput] = useState('')
  const [activeUsernameFilter, setActiveUsernameFilter] = useState<Username | null>(null)
  const [filterErrorMessage, setFilterErrorMessage] = useState('')
  const submissionQuery = useSubmissionListQuery(activeUsernameFilter)

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#edf4fb_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-5xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Qiwen Online Judge</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">Submissions</h1>
            <p className="text-sm text-slate-600">
              Signed in as {displayNameValue(user.displayName)} ({usernameValue(user.username)}).
            </p>
          </div>

          <AncestorNavigation />
        </div>

        {submissionQuery.errorMessage ? (
          <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{submissionQuery.errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        <Card className="mb-6 border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
          <CardHeader>
            <CardTitle className="text-xl text-slate-950">Filter submissions</CardTitle>
            <CardDescription>
              Enter a username to show submissions from that user among the submissions you are allowed to view.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="submission-username-filter">Username</Label>
              <Input
                id="submission-username-filter"
                value={filterInput}
                placeholder="Enter a username"
                onChange={(event) => {
                  setFilterInput(event.target.value)
                  setFilterErrorMessage('')
                }}
              />
            </div>

            {filterErrorMessage ? (
              <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                <AlertDescription className="text-rose-700">{filterErrorMessage}</AlertDescription>
              </Alert>
            ) : null}

            <div className="flex flex-wrap gap-3">
              <Button
                type="button"
                className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
                onClick={() => {
                  const trimmedInput = filterInput.trim()
                  if (!trimmedInput) {
                    setActiveUsernameFilter(null)
                    setFilterErrorMessage('')
                    return
                  }

                  const usernameResult = parseUsername(trimmedInput)
                  if (!usernameResult.ok) {
                    setFilterErrorMessage(usernameResult.error)
                    return
                  }

                  setActiveUsernameFilter(usernameResult.value)
                  setFilterInput(usernameValue(usernameResult.value))
                  setFilterErrorMessage('')
                }}
              >
                Apply filter
              </Button>
              <Button
                type="button"
                variant="outline"
                className="rounded-2xl border-slate-300 bg-white"
                onClick={() => {
                  setFilterInput('')
                  setActiveUsernameFilter(null)
                  setFilterErrorMessage('')
                }}
              >
                Clear filter
              </Button>
            </div>

            {activeUsernameFilter ? (
              <p className="text-sm text-slate-600">
                Showing submissions for <span className="font-medium text-slate-900">{usernameValue(activeUsernameFilter)}</span>.
              </p>
            ) : (
              <p className="text-sm text-slate-600">Showing submissions for all visible users.</p>
            )}
          </CardContent>
        </Card>

        {submissionQuery.isLoading ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">Loading submissions...</CardContent>
          </Card>
        ) : submissionQuery.submissions.length === 0 ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">No submissions have been recorded yet.</CardContent>
          </Card>
        ) : (
          <div className="space-y-4">
            {submissionQuery.submissions.map((submission) => (
              <Card
                key={submissionIdValue(submission.id)}
                className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]"
              >
                <CardHeader>
                  <div className="flex items-center gap-3">
                    <div className="flex size-12 items-center justify-center rounded-2xl bg-indigo-100 text-indigo-700">
                      <Files className="size-5" />
                    </div>
                    <div>
                      <CardTitle className="text-xl text-slate-950">
                        Submission #{submissionIdValue(submission.id)}
                      </CardTitle>
                      <CardDescription className="mt-2 text-sm font-medium text-slate-700">
                        {submissionIdValue(submission.id)}
                      </CardDescription>
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
                  <dl className="grid gap-4 text-sm text-slate-600 sm:grid-cols-2 lg:grid-cols-6">
                    <div>
                      <dt className="text-slate-500">Problem</dt>
                      <dd className="mt-1 font-medium text-slate-900">{submission.problemSlug}</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">Submitter</dt>
                      <dd className="mt-1 font-medium text-slate-900">{submission.submitterUsername}</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">Language</dt>
                      <dd className="mt-1 font-medium text-slate-900">
                        {submissionLanguageLabel(submission.language)}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">Status</dt>
                      <dd className="mt-1 font-medium text-slate-900">{submissionStatusLabel(submission.status)}</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">Verdict</dt>
                      <dd className="mt-1 font-medium text-slate-900">{submissionVerdictLabel(submission.verdict)}</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">Submitted at</dt>
                      <dd className="mt-1 font-medium text-slate-900">
                        {new Date(submission.submittedAt).toLocaleString()}
                      </dd>
                    </div>
                  </dl>

                  <Button asChild className="rounded-2xl bg-indigo-300 text-indigo-950 hover:bg-indigo-400">
                    <Link to={`/submissions/${submissionIdValue(submission.id)}`}>
                      View Source Code
                      <ArrowRight className="size-4" />
                    </Link>
                  </Button>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </section>
    </main>
  )
}
