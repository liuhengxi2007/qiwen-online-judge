import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, LogOut, PencilLine, ScrollText, Trash2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { Input } from '@/components/ui/input'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import {
  parseProblemSlug,
  problemSlugValue,
  problemStatementTextValue,
  problemTitleValue,
} from '@/features/problem/domain/problem'
import { useProblemDetailPageModel } from '@/features/problem/hooks/use-problem-detail-page-model'
import { usePageTitle } from '@/shared/hooks/use-page-title'

export function ProblemDetailPage() {
  usePageTitle('Qiwen Online Judge - Problem Detail')
  const { session: user, signOut, navigationIntent } = useSessionGuard()
  const navigate = useNavigate()
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

  const model = useProblemDetailPageModel(slugResult.value)
  const canManageProblem = user.siteManager || user.problemManager

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#edf5f1_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-5xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Qiwen Online Judge</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">Problem Detail</h1>
            <p className="text-sm text-slate-600">
              Signed in as {displayNameValue(user.displayName)} ({usernameValue(user.username)}).
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Button asChild variant="outline" className="rounded-full border-slate-300 bg-white">
              <Link to="/problems">
                <ArrowLeft className="size-4" />
                Back to Problems
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

        {model.isLoading ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">Loading problem detail...</CardContent>
          </Card>
        ) : model.problem ? (
          <div className="space-y-6">
            <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
              <CardHeader>
                <div className="flex items-center gap-3">
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                    <ScrollText className="size-5" />
                  </div>
                  <div>
                    <CardTitle className="text-2xl text-slate-950">{problemTitleValue(model.problem.title)}</CardTitle>
                    <CardDescription className="mt-2 font-mono text-sm text-slate-500">
                      {problemSlugValue(model.problem.slug)}
                    </CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-5">
                <div className="flex flex-wrap items-center gap-3">
                  <Badge variant="secondary">{model.problem.visibility}</Badge>
                  <Badge variant="outline">{model.problem.status}</Badge>
                </div>
                <pre className="whitespace-pre-wrap break-words rounded-3xl bg-slate-50 px-6 py-6 font-['Georgia'] text-sm leading-7 text-slate-700">
                  {problemStatementTextValue(model.problem.statement)}
                </pre>
                <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
                  Owner {usernameValue(model.problem.ownerUsername)}
                </p>
              </CardContent>
            </Card>

            {canManageProblem ? (
              <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
                <CardHeader>
                  <div className="flex items-center gap-3">
                    <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
                      <PencilLine className="size-5" />
                    </div>
                    <div>
                      <CardTitle className="text-xl text-slate-950">Edit Problem</CardTitle>
                      <CardDescription>Update the problem title, plain-text statement, and visibility.</CardDescription>
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="space-y-5">
                  <div className="space-y-2">
                    <Label htmlFor="problem-title">Title</Label>
                    <Input
                      id="problem-title"
                      value={model.title}
                      onChange={(event) => model.setTitle(event.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="problem-statement">Plain-text statement</Label>
                    <Textarea
                      id="problem-statement"
                      className="min-h-64"
                      value={model.statement}
                      onChange={(event) => model.setStatement(event.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>Visibility</Label>
                    <Select value={model.visibility} onValueChange={(value) => model.setVisibility(value as 'private' | 'group' | 'public')}>
                      <SelectTrigger>
                        <SelectValue placeholder="Select visibility" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="private">Private</SelectItem>
                        <SelectItem value="group">Group</SelectItem>
                        <SelectItem value="public">Public</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <Button
                    type="button"
                    className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
                    disabled={model.isSaving}
                    onClick={() => {
                      void model.save()
                    }}
                  >
                    {model.isSaving ? 'Saving changes...' : 'Save changes'}
                  </Button>
                </CardContent>
              </Card>
            ) : null}

            {canManageProblem ? (
              <Card className="border-rose-200 bg-rose-50/60 shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
                <CardHeader>
                  <div className="flex items-center gap-3">
                    <div className="flex size-12 items-center justify-center rounded-2xl bg-rose-100 text-rose-700">
                      <Trash2 className="size-5" />
                    </div>
                    <div>
                      <CardTitle className="text-xl text-rose-950">Delete Problem</CardTitle>
                      <CardDescription>This removes the problem and any existing problem set links to it.</CardDescription>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <Button
                    type="button"
                    variant="outline"
                    className="rounded-2xl border-rose-300 bg-white text-rose-700 hover:bg-rose-100 hover:text-rose-800"
                    disabled={model.isDeleting}
                    onClick={() => {
                      const confirmed = window.confirm('Delete this problem? This action cannot be undone.')
                      if (!confirmed) {
                        return
                      }

                      void model.deleteCurrentProblem().then((deleted) => {
                        if (deleted) {
                          void navigate('/problems')
                        }
                      })
                    }}
                  >
                    {model.isDeleting ? 'Deleting...' : 'Delete problem'}
                  </Button>
                </CardContent>
              </Card>
            ) : null}
          </div>
        ) : null}
      </section>
    </main>
  )
}
