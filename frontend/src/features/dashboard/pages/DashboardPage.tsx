import { Link, Navigate } from 'react-router-dom'
import { BookCopy, FileText, Files, NotebookPen, Users, UsersRound } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { displayNameValue } from '@/features/auth/domain/auth'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { useI18n } from '@/shared/i18n/i18n'

export function DashboardPage() {
  const { t } = useI18n()
  usePageTitle(t('dashboard.title'))
  const { session: user, siteManagerSession, navigationIntent } = useSessionGuard()

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
        </div>

        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-rose-100 text-rose-700">
                  <BookCopy className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">{t('dashboard.problemSets.title')}</CardTitle>
                  <CardDescription>{t('dashboard.problemSets.description')}</CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <Button asChild className="rounded-2xl bg-rose-300 text-rose-950 hover:bg-rose-400">
                <Link to="/problem-sets">{t('dashboard.problemSets.open')}</Link>
              </Button>
            </CardContent>
          </Card>

          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardHeader>
              <div className="flex items-center gap-3">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-orange-100 text-orange-700">
                  <NotebookPen className="size-5" />
                </div>
                <div>
                  <CardTitle className="text-xl text-slate-950">{t('dashboard.blogs.title')}</CardTitle>
                  <CardDescription>{t('dashboard.blogs.description')}</CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <Button asChild className="rounded-2xl bg-orange-300 text-orange-950 hover:bg-orange-400">
                <Link to="/blogs">{t('dashboard.blogs.open')}</Link>
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
                  <CardTitle className="text-xl text-slate-950">{t('dashboard.problems.title')}</CardTitle>
                  <CardDescription>{t('dashboard.problems.description')}</CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <Button asChild className="rounded-2xl bg-emerald-300 text-emerald-950 hover:bg-emerald-400">
                <Link to="/problems">{t('dashboard.problems.open')}</Link>
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
                  <CardTitle className="text-xl text-slate-950">{t('dashboard.submissions.title')}</CardTitle>
                  <CardDescription>{t('dashboard.submissions.description')}</CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <Button asChild className="rounded-2xl bg-indigo-300 text-indigo-950 hover:bg-indigo-400">
                <Link to="/submissions">{t('dashboard.submissions.open')}</Link>
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
                  <CardTitle className="text-xl text-slate-950">{t('dashboard.userGroups.title')}</CardTitle>
                  <CardDescription>{t('dashboard.userGroups.description')}</CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <Button asChild className="rounded-2xl bg-sky-300 text-sky-950 hover:bg-sky-400">
                <Link to="/user-groups">{t('dashboard.userGroups.open')}</Link>
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
                    <CardTitle className="text-xl text-slate-950">{t('dashboard.siteManage.title')}</CardTitle>
                    <CardDescription>{t('dashboard.siteManage.description')}</CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <Button asChild className="rounded-2xl bg-amber-300 text-amber-950 hover:bg-amber-400">
                  <Link to="/site-manage">{t('dashboard.siteManage.open')}</Link>
                </Button>
              </CardContent>
            </Card>
          ) : null}
        </div>
      </section>
    </main>
  )
}
