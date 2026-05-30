import { Link, Navigate } from 'react-router-dom'
import type { LucideIcon } from 'lucide-react'
import { BookCopy, FileText, Files, NotebookPen, Trophy, Users, UsersRound } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { AccountActions } from '@/pages/components/AccountActions'
import { formatUserDisplayLabel } from '@/pages/objects/UserDisplayLabel'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { useI18n } from '@/system/i18n/use-i18n'

type DashboardAction = {
  title: string
  description: string
  openLabel: string
  to: string
  icon: LucideIcon
  iconClassName: string
  buttonClassName: string
}

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
            <h1 className="page-title-font mt-2 text-4xl font-semibold tracking-tight text-slate-950">
              {t('dashboard.welcome', {
                displayName: formatUserDisplayLabel(user, user.preferences.displayMode),
              })}
            </h1>
          </div>
          <AccountActions showSignOutLabel />
        </div>

        <DashboardActionGrid showSiteManage={Boolean(siteManagerSession)} />
      </section>
    </main>
  )
}

function DashboardActionGrid({ showSiteManage }: { showSiteManage: boolean }) {
  const { t } = useI18n()
  const actions: DashboardAction[] = [
    {
      title: t('dashboard.problems.title'),
      description: t('dashboard.problems.description'),
      openLabel: t('dashboard.problems.open'),
      to: '/problems',
      icon: FileText,
      iconClassName: 'bg-emerald-100 text-emerald-700',
      buttonClassName: 'bg-emerald-300 text-emerald-950 hover:bg-emerald-400',
    },
    {
      title: t('dashboard.problemSets.title'),
      description: t('dashboard.problemSets.description'),
      openLabel: t('dashboard.problemSets.open'),
      to: '/problem-sets',
      icon: BookCopy,
      iconClassName: 'bg-rose-100 text-rose-700',
      buttonClassName: 'bg-rose-300 text-rose-950 hover:bg-rose-400',
    },
    {
      title: t('dashboard.submissions.title'),
      description: t('dashboard.submissions.description'),
      openLabel: t('dashboard.submissions.open'),
      to: '/submissions',
      icon: Files,
      iconClassName: 'bg-indigo-100 text-indigo-700',
      buttonClassName: 'bg-indigo-300 text-indigo-950 hover:bg-indigo-400',
    },
    {
      title: t('dashboard.blogs.title'),
      description: t('dashboard.blogs.description'),
      openLabel: t('dashboard.blogs.open'),
      to: '/blogs',
      icon: NotebookPen,
      iconClassName: 'bg-orange-100 text-orange-700',
      buttonClassName: 'bg-orange-300 text-orange-950 hover:bg-orange-400',
    },
    {
      title: t('dashboard.ranklist.title'),
      description: t('dashboard.ranklist.description'),
      openLabel: t('dashboard.ranklist.open'),
      to: '/ranklist',
      icon: Trophy,
      iconClassName: 'bg-amber-100 text-amber-700',
      buttonClassName: 'bg-amber-300 text-amber-950 hover:bg-amber-400',
    },
    {
      title: t('dashboard.userGroups.title'),
      description: t('dashboard.userGroups.description'),
      openLabel: t('dashboard.userGroups.open'),
      to: '/user-groups',
      icon: UsersRound,
      iconClassName: 'bg-sky-100 text-sky-700',
      buttonClassName: 'bg-sky-300 text-sky-950 hover:bg-sky-400',
    },
  ]

  if (showSiteManage) {
    actions.push({
      title: t('dashboard.siteManage.title'),
      description: t('dashboard.siteManage.description'),
      openLabel: t('dashboard.siteManage.open'),
      to: '/site-manage',
      icon: Users,
      iconClassName: 'bg-fuchsia-100 text-fuchsia-700',
      buttonClassName: 'bg-fuchsia-300 text-fuchsia-950 hover:bg-fuchsia-400',
    })
  }

  return (
    <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
      {actions.map((action) => (
        <DashboardActionCard key={action.to} action={action} />
      ))}
    </div>
  )
}

function DashboardActionCard({ action }: { action: DashboardAction }) {
  const Icon = action.icon

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className={`flex size-12 items-center justify-center rounded-2xl ${action.iconClassName}`}>
            <Icon className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl text-slate-950">{action.title}</CardTitle>
            <CardDescription>{action.description}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <Button asChild className={`rounded-2xl ${action.buttonClassName}`}>
          <Link to={action.to}>{action.openLabel}</Link>
        </Button>
      </CardContent>
    </Card>
  )
}
