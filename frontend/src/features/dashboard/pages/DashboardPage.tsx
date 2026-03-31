import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { BookCopy, FileText, LogOut, Settings, ShieldCheck, Users } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'

export function DashboardPage() {
  usePageTitle('Qiwen Online Judge')
  const [searchParams] = useSearchParams()
  const { session: user, siteManagerSession, signOut, navigationIntent } = useSessionGuard()
  const notice = searchParams.get('notice')
  const noticeMessage =
    notice === 'site-manage-denied'
      ? 'Site management is available only to accounts with the site manager permission.'
      : notice === 'settings-route-corrected'
        ? 'The settings route was corrected to match your signed-in username.'
        : null

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#eef2f7_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-4xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">Qiwen Online Judge</p>
            <h1 className="mt-2 font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">
              Welcome back, {displayNameValue(user.displayName)}
            </h1>
          </div>
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

        {noticeMessage ? (
          <Alert className="mb-8 rounded-3xl border-sky-200 bg-sky-50 text-sky-800">
            <AlertDescription>{noticeMessage}</AlertDescription>
          </Alert>
        ) : null}

        <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                <ShieldCheck className="size-5" />
              </div>
              <div>
                <CardTitle className="text-xl text-slate-950">Qiwen Online Judge Console</CardTitle>
                <CardDescription>
                  Your account is connected to the backend service and ready for management tasks.
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            <div className="rounded-2xl bg-slate-50 p-5">
              <p className="text-sm text-slate-500">Display name</p>
              <p className="mt-2 text-lg font-semibold text-slate-900">
                {displayNameValue(user.displayName)}
              </p>
            </div>
            <div className="rounded-2xl bg-slate-50 p-5">
              <p className="text-sm text-slate-500">Username</p>
              <p className="mt-2 text-lg font-semibold text-slate-900">{usernameValue(user.username)}</p>
            </div>
          </CardContent>
        </Card>

        <div className="mt-8 grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-700">
                  <Settings className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">User Settings</CardTitle>
                  <CardDescription>
                    Review your account route and permission summary.
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-sm leading-7 text-slate-600">
                Open your dedicated settings page under the username-scoped route.
              </p>
              <Button asChild className="rounded-2xl bg-sky-600 text-sky-50 hover:bg-sky-700">
                <Link to={`/user/${usernameValue(user.username)}/settings`}>Open User Settings</Link>
              </Button>
            </CardContent>
          </Card>

          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-rose-100 text-rose-700">
                  <BookCopy className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">Problem Sets</CardTitle>
                  <CardDescription>
                    Manage draft problem sets and typed metadata in the new domain module.
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-sm leading-7 text-slate-600">
                The problemset module is now wired end-to-end with backend persistence and a dedicated frontend route.
              </p>
              <Button asChild className="rounded-2xl bg-rose-600 text-rose-50 hover:bg-rose-700">
                <Link to="/problem-sets">Open Problem Sets</Link>
              </Button>
            </CardContent>
          </Card>

          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                  <FileText className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">Problems</CardTitle>
                  <CardDescription>
                    Manage draft problems with typed metadata and plain-text statements.
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-sm leading-7 text-slate-600">
                The first problem domain supports creation, listing, and whitespace-preserving statement display.
              </p>
              <Button asChild className="rounded-2xl bg-emerald-600 text-emerald-50 hover:bg-emerald-700">
                <Link to="/problems">Open Problems</Link>
              </Button>
            </CardContent>
          </Card>

          {siteManagerSession ? (
            <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
              <CardHeader>
                <div className="flex items-center gap-3">
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
                    <Users className="size-5" />
                  </div>
                  <div>
                    <CardTitle className="text-xl text-slate-950">Site Manage</CardTitle>
                    <CardDescription>
                      Review users and site-level permissions in a dedicated management page.
                    </CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <p className="text-sm leading-7 text-slate-600">
                  User management has moved out of the dashboard into its own route.
                </p>
                <Button asChild className="rounded-2xl bg-amber-500 text-stone-950 hover:bg-amber-400">
                  <Link to="/site-manage">Open Site Management</Link>
                </Button>
              </CardContent>
            </Card>
          ) : null}
        </div>
      </section>
    </main>
  )
}
