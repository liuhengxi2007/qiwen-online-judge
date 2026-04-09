import { Link, Navigate } from 'react-router-dom'
import { FilePlus2, LibraryBig } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import {
  problemSlugValue,
  problemTitleValue,
} from '@/features/problem/domain/problem'
import { useProblemPageModel } from '@/features/problem/hooks/use-problem-page-model'
import { resourceAccessBadgeLabel } from '@/shared/domain/resource-lifecycle'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { usePageTitle } from '@/shared/hooks/use-page-title'

export function ProblemPage() {
  usePageTitle('Qiwen Online Judge - Problems')
  const { session: user, navigationIntent } = useSessionGuard()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const canCreate = user.siteManager || user.problemManager
  const model = useProblemPageModel()

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#edf5f1_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Qiwen Online Judge</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">Problems</h1>
            <p className="text-sm text-slate-600">
              Signed in as {displayNameValue(user.displayName)} ({usernameValue(user.username)}).
            </p>
          </div>

          <AncestorNavigation />
        </div>

        <div className="space-y-6">
          {model.errorMessage ? (
            <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
              <AlertDescription className="text-rose-700">{model.errorMessage}</AlertDescription>
            </Alert>
          ) : null}

          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                    <LibraryBig className="size-5" />
                  </div>
                  <div>
                    <CardTitle className="text-xl text-slate-950">Current Problems</CardTitle>
                    <CardDescription>
                      Browse available problems and open each statement in its full formatted view.
                    </CardDescription>
                  </div>
                </div>
                {canCreate ? (
                  <Button asChild className="rounded-2xl bg-emerald-300 text-emerald-950 hover:bg-emerald-400">
                    <Link to="/problems/new">
                      <FilePlus2 className="size-4" />
                      Create problem
                    </Link>
                  </Button>
                ) : null}
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              {model.isLoading ? (
                <p className="text-sm text-slate-500">Loading problems...</p>
              ) : model.problems.length === 0 ? (
                <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center">
                  <p className="text-base font-medium text-slate-900">No problems yet.</p>
                  <p className="mt-2 text-sm leading-7 text-slate-600">
                    Create the first problem to start building your problem library.
                  </p>
                </div>
              ) : (
                model.problems.map((problem) => (
                  <div key={problem.id} className="rounded-3xl border border-slate-200 bg-slate-50 p-5">
                    <div className="flex flex-wrap items-center gap-3">
                      <Link className="text-lg font-semibold text-slate-950 hover:underline" to={`/problems/${problemSlugValue(problem.slug)}`}>
                        {problemTitleValue(problem.title)}
                      </Link>
                      <Badge variant="secondary">{resourceAccessBadgeLabel(problem.accessPolicy)}</Badge>
                    </div>
                    <p className="mt-2 font-mono text-sm text-slate-500">{problemSlugValue(problem.slug)}</p>
                    <p className="mt-4 text-xs uppercase tracking-[0.18em] text-slate-400">
                      Owner {usernameValue(problem.ownerUsername)}
                    </p>
                  </div>
                ))
              )}
            </CardContent>
          </Card>
        </div>
      </section>
    </main>
  )
}
