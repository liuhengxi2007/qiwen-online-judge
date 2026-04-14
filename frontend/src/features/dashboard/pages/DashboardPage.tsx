import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { BookCopy, FileText, Files, LogOut, Settings, ShieldCheck, Users, UsersRound } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { useI18n } from '@/shared/i18n/i18n'

export function DashboardPage() {
  const { t } = useI18n()
  usePageTitle(t('dashboard.title'))
  const [searchParams] = useSearchParams()
  const { session: user, siteManagerSession, signOut, navigationIntent } = useSessionGuard()
  const notice = searchParams.get('notice')
  const noticeMessage =
    notice === 'site-manage-denied'
      ? t('dashboard.notice.siteManageDenied')
      : notice === 'settings-route-corrected'
        ? t('dashboard.notice.settingsRouteCorrected')
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
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="mt-2 font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">
              {t('dashboard.welcome', { displayName: displayNameValue(user.displayName) })}
            </h1>
          </div>
          <AncestorNavigation />
        </div>

        {noticeMessage ? (
          <Alert className="mb-8 rounded-3xl border-sky-200 bg-sky-50 text-sky-800">
            <AlertDescription>{noticeMessage}</AlertDescription>
          </Alert>
        ) : null}

        <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
          <CardHeader>
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                  <ShieldCheck className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">{t('dashboard.console.title')}</CardTitle>
                  <CardDescription>
                    {t('dashboard.console.description')}
                  </CardDescription>
                </div>
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
                {t('dashboard.signOut')}
              </Button>
            </div>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            <div className="rounded-2xl bg-slate-50 p-5">
              <p className="text-sm text-slate-500">{t('dashboard.displayName')}</p>
              <p className="mt-2 text-lg font-semibold text-slate-900">
                {displayNameValue(user.displayName)}
              </p>
            </div>
            <div className="rounded-2xl bg-slate-50 p-5">
              <p className="text-sm text-slate-500">{t('dashboard.username')}</p>
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
                    Update your profile information, manage your password, and review your account permissions.
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <Button asChild className="rounded-2xl bg-violet-300 text-violet-950 hover:bg-violet-400">
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
                    Organize problems into curated sets for contests, training, or private review, and manage linked problems in one place.
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <Button asChild className="rounded-2xl bg-rose-300 text-rose-950 hover:bg-rose-400">
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
                    Create, edit, and review problems with formatted statements, access controls, and full problem details.
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <Button asChild className="rounded-2xl bg-emerald-300 text-emerald-950 hover:bg-emerald-400">
                <Link to="/problems">Open Problems</Link>
              </Button>
            </CardContent>
          </Card>

          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-indigo-100 text-indigo-700">
                  <Files className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">Submissions</CardTitle>
                  <CardDescription>
                    Browse past submissions, review their status and verdict, and inspect the source code you sent.
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <Button asChild className="rounded-2xl bg-indigo-300 text-indigo-950 hover:bg-indigo-400">
                <Link to="/submissions">Open Submissions</Link>
              </Button>
            </CardContent>
          </Card>

          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
                  <UsersRound className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">User Groups</CardTitle>
                  <CardDescription>
                    Create groups, manage members, and organize collaboration and access control more easily.
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <Button asChild className="rounded-2xl bg-sky-300 text-sky-950 hover:bg-sky-400">
                <Link to="/user-groups">Open User Groups</Link>
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
                    <CardTitle className="text-xl text-slate-950">Site Management</CardTitle>
                    <CardDescription>
                      Review registered users, adjust site-wide permissions, and maintain user accounts.
                    </CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <Button asChild className="rounded-2xl bg-amber-300 text-amber-950 hover:bg-amber-400">
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
