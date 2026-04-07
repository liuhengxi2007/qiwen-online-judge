import { Link, Navigate, useParams } from 'react-router-dom'
import { ArrowDownToLine, ArrowLeft, Eraser, FileUp, HardDriveUpload, LogOut, Trash2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { problemDataDownloadUrl } from '@/features/problem/api/problem-client'
import {
  parseProblemSlug,
  problemDataFilenameValue,
  problemSlugValue,
  problemTitleValue,
} from '@/features/problem/domain/problem'
import { useProblemDataPageModel } from '@/features/problem/hooks/use-problem-data-page-model'
import { ConfirmActionDialog } from '@/shared/components/confirm-action-dialog'
import { usePageTitle } from '@/shared/hooks/use-page-title'

export function ProblemDataPage() {
  usePageTitle('Qiwen Online Judge - Problem Data')
  const { session: user, signOut, navigationIntent } = useSessionGuard()
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

  if (!(user.siteManager || user.problemManager)) {
    return <Navigate replace to={`/problems/${problemSlugValue(slugResult.value)}`} />
  }

  const model = useProblemDataPageModel(slugResult.value)

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#edf5f1_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-3xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Qiwen Online Judge</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">Problem Data</h1>
            <p className="text-sm text-slate-600">
              Signed in as {displayNameValue(user.displayName)} ({usernameValue(user.username)}).
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Button asChild variant="outline" className="rounded-full border-slate-300 bg-white">
              <Link to={`/problems/${problemSlugValue(slugResult.value)}`}>
                <ArrowLeft className="size-4" />
                Back to Problem
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

        {model.isProblemLoading ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">Loading problem data...</CardContent>
          </Card>
        ) : model.problem ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
                  <HardDriveUpload className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">{problemTitleValue(model.problem.title)}</CardTitle>
                  <CardDescription className="mt-2 font-mono text-sm text-slate-500">
                    {problemSlugValue(model.problem.slug)}
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-5">
              <div className="grid gap-4 sm:grid-cols-3">
                <div className="rounded-2xl bg-slate-50 px-5 py-4">
                  <p className="text-sm text-slate-500">Time limit</p>
                  <p className="mt-2 text-lg font-semibold text-slate-900">{model.problem.timeLimitMs} ms</p>
                </div>
                <div className="rounded-2xl bg-slate-50 px-5 py-4">
                  <p className="text-sm text-slate-500">Space limit</p>
                  <p className="mt-2 text-lg font-semibold text-slate-900">{model.problem.spaceLimitMb} MB</p>
                </div>
                <div className="rounded-2xl bg-slate-50 px-5 py-4">
                  <p className="text-sm text-slate-500">Latest uploaded file</p>
                  <p className="mt-2 text-base font-medium text-slate-900">
                    {model.problem.data ? problemDataFilenameValue(model.problem.data) : 'No data uploaded'}
                  </p>
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="problem-data-file">Upload file</Label>
                <Input
                  id="problem-data-file"
                  type="file"
                  className="rounded-2xl"
                  onChange={(event) => {
                    model.setSelectedFile(event.target.files?.[0] ?? null)
                    model.setErrorMessage('')
                    model.setSuccessMessage('')
                  }}
                />
              </div>

              {model.errorMessage ? (
                <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
                  <AlertDescription className="text-rose-700">{model.errorMessage}</AlertDescription>
                </Alert>
              ) : null}

              {model.successMessage ? (
                <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
                  <AlertDescription className="text-emerald-700">{model.successMessage}</AlertDescription>
                </Alert>
              ) : null}

              <Button
                type="button"
                disabled={model.isUploading || model.selectedFile === null}
                className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
                onClick={() => {
                  void model.uploadSelectedFile()
                }}
              >
                <FileUp className="size-4" />
                {model.isUploading ? 'Uploading data...' : 'Upload data file'}
              </Button>

              <div className="space-y-3 rounded-3xl border border-slate-200 bg-slate-50 px-5 py-5">
                <div>
                  <p className="text-sm font-medium text-slate-900">Uploaded data files</p>
                  <p className="mt-1 text-sm text-slate-500">
                    Files are stored under the problem folder and can be downloaded here.
                  </p>
                </div>

                {model.isLoadingFiles ? (
                  <p className="text-sm text-slate-500">Loading files...</p>
                ) : model.dataFiles.length === 0 ? (
                  <p className="text-sm text-slate-500">No data files uploaded yet.</p>
                ) : (
                  <div className="space-y-3">
                    <div className="flex justify-end">
                      <ConfirmActionDialog
                        title="Clear all data files?"
                        description="This currently deletes every uploaded data file for the problem. This action cannot be undone."
                        confirmLabel={model.isClearingAll ? 'Clearing...' : 'Clear all files'}
                        destructive
                        onConfirm={() => {
                          void model.clearAllDataFiles()
                        }}
                        trigger={
                          <Button
                            type="button"
                            variant="outline"
                            disabled={model.isClearingAll}
                            className="rounded-2xl border-rose-200 bg-white text-rose-700 hover:bg-rose-50 hover:text-rose-800"
                          >
                            <Eraser className="size-4" />
                            {model.isClearingAll ? 'Clearing...' : 'Clear all files'}
                          </Button>
                        }
                      />
                    </div>
                    {model.dataFiles.map((filename) => (
                      <div
                        key={problemDataFilenameValue(filename)}
                        className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-4 sm:flex-row sm:items-center sm:justify-between"
                      >
                        <p className="text-sm font-medium text-slate-900">{problemDataFilenameValue(filename)}</p>
                        <div className="flex flex-col gap-3 sm:flex-row">
                          <Button asChild variant="outline" className="rounded-2xl border-slate-300 bg-white">
                            <a href={problemDataDownloadUrl(slugResult.value, filename)}>
                              <ArrowDownToLine className="size-4" />
                              Download
                            </a>
                          </Button>
                          <Button
                            type="button"
                            variant="outline"
                            disabled={model.deletingFilename === filename}
                            className="rounded-2xl border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 hover:text-rose-800"
                            onClick={() => {
                              void model.deleteDataFile(filename)
                            }}
                          >
                            <Trash2 className="size-4" />
                            {model.deletingFilename === filename ? 'Deleting...' : 'Delete'}
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        ) : (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">
              {model.problemErrorMessage || 'Unable to load problem details.'}
            </AlertDescription>
          </Alert>
        )}
      </section>
    </main>
  )
}
