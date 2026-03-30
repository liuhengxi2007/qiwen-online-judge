import { Link, Navigate, useParams } from 'react-router-dom'
import { ArrowLeft, Link2, LogOut, Rows3 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import {
  parseProblemSetSlug,
  problemSetDescriptionValue,
  problemSetProblemPositionValue,
  problemSetSlugValue,
  problemSetTitleValue,
} from '@/features/problemset/domain/problemset'
import { useProblemSetDetailPageModel } from '@/features/problemset/hooks/use-problemset-detail-page-model'
import { usePageTitle } from '@/shared/hooks/use-page-title'

export function ProblemSetDetailPage() {
  usePageTitle('Qiwen Online Judge - Problem Set Detail')
  const { session: user, signOut, navigationIntent } = useSessionGuard()
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

  const canManageProblems = user.siteManager || user.problemManager
  const model = useProblemSetDetailPageModel(slugResult.value, canManageProblems)

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#ecf3fb_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-5xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Qiwen Online Judge</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">Problem Set Detail</h1>
            <p className="text-sm text-slate-600">
              Signed in as {displayNameValue(user.displayName)} ({usernameValue(user.username)}).
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Button asChild variant="outline" className="rounded-full border-slate-300 bg-white">
              <Link to="/problem-sets">
                <ArrowLeft className="size-4" />
                Back to Problem Sets
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
            <CardContent className="py-10 text-sm text-slate-500">Loading problem set detail...</CardContent>
          </Card>
        ) : model.problemSet ? (
          <div className="space-y-6">
            <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
              <CardHeader>
                <CardTitle className="text-2xl text-slate-950">{problemSetTitleValue(model.problemSet.title)}</CardTitle>
                <CardDescription className="mt-2 font-mono text-sm text-slate-500">
                  {problemSetSlugValue(model.problemSet.slug)}
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex flex-wrap items-center gap-3">
                  <Badge variant="secondary">{model.problemSet.visibility}</Badge>
                  <Badge variant="outline">{model.problemSet.status}</Badge>
                </div>
                <p className="text-sm leading-7 text-slate-600">
                  {problemSetDescriptionValue(model.problemSet.description) || 'No description provided.'}
                </p>
                <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
                  Owner {usernameValue(model.problemSet.ownerUsername)}
                </p>
              </CardContent>
            </Card>

            <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
              <CardHeader>
                <div className="flex items-center gap-3">
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-700">
                    <Rows3 className="size-5" />
                  </div>
                  <div>
                    <CardTitle className="text-xl text-slate-950">Linked Problems</CardTitle>
                    <CardDescription>Detailed linked problem information lives here instead of the list page.</CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                {model.problemSet.problems.length === 0 ? (
                  <p className="text-sm text-slate-500">No problems linked yet.</p>
                ) : (
                  model.problemSet.problems.map((problem) => (
                    <div key={problem.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                      <div className="flex flex-wrap items-center gap-2">
                        <Badge variant="outline">#{problemSetProblemPositionValue(problem.position)}</Badge>
                        <Link className="text-sm font-medium text-slate-900 hover:underline" to={`/problems/${problem.slug}`}>
                          {problem.title}
                        </Link>
                      </div>
                      <p className="mt-1 font-mono text-xs text-slate-500">{problem.slug}</p>
                    </div>
                  ))
                )}
              </CardContent>
            </Card>

            {canManageProblems ? (
              <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
                <CardHeader>
                  <div className="flex items-center gap-3">
                    <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
                      <Link2 className="size-5" />
                    </div>
                    <div>
                      <CardTitle className="text-xl text-slate-950">Link Problem</CardTitle>
                      <CardDescription>Add an existing problem into this problem set by slug.</CardDescription>
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="space-y-3">
                  <div className="space-y-2">
                    <Label htmlFor="link-problem-slug">Problem slug</Label>
                    <Input
                      id="link-problem-slug"
                      value={model.linkProblemSlug}
                      placeholder="two-sum-intro"
                      onChange={(event) => model.setLinkProblemSlug(event.target.value)}
                    />
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    className="rounded-2xl border-slate-300 bg-white"
                    disabled={model.activeLink}
                    onClick={() => {
                      void model.attachProblem()
                    }}
                  >
                    {model.activeLink ? 'Linking problem...' : 'Link problem'}
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
