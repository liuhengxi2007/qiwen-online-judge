import { Link, Navigate } from 'react-router-dom'
import { BookPlus, Layers3, LogOut } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import {
  problemSetDescriptionValue,
  problemSetSlugValue,
  problemSetTitleValue,
} from '@/features/problemset/domain/problemset'
import { useProblemSetPageModel } from '@/features/problemset/hooks/use-problemset-page-model'
import { resourceAccessBadgeLabel } from '@/shared/domain/resource-lifecycle'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { usePageTitle } from '@/shared/hooks/use-page-title'

export function ProblemSetPage() {
  usePageTitle('Qiwen Online Judge - Problem Sets')
  const { session: user, signOut, navigationIntent } = useSessionGuard()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const canCreate = user.siteManager || user.problemManager
  const model = useProblemSetPageModel()

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#ecf3fb_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Qiwen Online Judge</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">Problem Sets</h1>
            <p className="text-sm text-slate-600">
              Signed in as {displayNameValue(user.displayName)} ({usernameValue(user.username)}).
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <AncestorNavigation />
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
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-700">
                    <Layers3 className="size-5" />
                  </div>
                  <div>
                    <CardTitle className="text-xl text-slate-950">Current Problem Sets</CardTitle>
                    <CardDescription>
                      This page shows problem set summaries only. Detailed linked problems live on the detail page.
                    </CardDescription>
                  </div>
                </div>
                {canCreate ? (
                  <Button asChild className="rounded-2xl bg-sky-600 text-sky-50 hover:bg-sky-700">
                    <Link to="/problem-sets/new">
                      <BookPlus className="size-4" />
                      Create problem set
                    </Link>
                  </Button>
                ) : null}
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              {model.isLoading ? (
                <p className="text-sm text-slate-500">Loading problem sets...</p>
              ) : model.problemSets.length === 0 ? (
                <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center">
                  <p className="text-base font-medium text-slate-900">No problem sets yet.</p>
                  <p className="mt-2 text-sm leading-7 text-slate-600">
                    This domain is now wired end-to-end. Create the first draft problem set from the left panel.
                  </p>
                </div>
              ) : (
                model.problemSets.map((problemSet) => (
                  <div key={problemSet.id} className="rounded-3xl border border-slate-200 bg-slate-50 p-5">
                    <div className="flex flex-wrap items-center gap-3">
                      <Link className="text-lg font-semibold text-slate-950 hover:underline" to={`/problem-sets/${problemSetSlugValue(problemSet.slug)}`}>
                        {problemSetTitleValue(problemSet.title)}
                      </Link>
                      <Badge variant="secondary">{resourceAccessBadgeLabel(problemSet.accessPolicy)}</Badge>
                    </div>
                    <p className="mt-2 font-mono text-sm text-slate-500">{problemSetSlugValue(problemSet.slug)}</p>
                    <p className="mt-3 text-sm leading-7 text-slate-600">
                      {problemSetDescriptionValue(problemSet.description) || 'No description provided.'}
                    </p>
                    <p className="mt-4 text-xs uppercase tracking-[0.18em] text-slate-400">
                      Owner {usernameValue(problemSet.ownerUsername)}
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
