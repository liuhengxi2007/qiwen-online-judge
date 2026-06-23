import { Link, Navigate } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { AccountActions } from '@/pages/components/AccountActions'
import type { AppDashboardEntryGroup, AppEntry } from '@/pages/objects/AppEntryCatalog'
import { getDashboardEntryGroups } from '@/pages/objects/AppEntryCatalog'
import { appModuleThemes } from '@/pages/objects/AppModuleTheme'
import { formatUserDisplayLabel } from '@/pages/objects/UserDisplayLabel'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 登录后仪表盘页，展示常用业务入口并根据站点管理员会话追加管理入口。
 */
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
      <section className="mx-auto max-w-6xl">
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

/**
 * 仪表盘入口分组列表，根据 showSiteManage 决定是否包含站点管理分组。
 */
function DashboardActionGrid({
  showSiteManage,
}: {
  showSiteManage: boolean
}) {
  const groups = getDashboardEntryGroups(showSiteManage)

  return (
    <div className="space-y-8">
      {groups.map((group) => (
        <DashboardActionGroup key={group.id} group={group} />
      ))}
    </div>
  )
}

/**
 * 仪表盘入口分组，渲染分组标题和组内入口卡片。
 */
function DashboardActionGroup({ group }: { group: AppDashboardEntryGroup }) {
  const { t } = useI18n()

  return (
    <section className="space-y-4">
      <h2 className="text-lg font-semibold text-slate-900">{t(group.titleKey)}</h2>
      <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        {group.entries.map((entry) => (
          <DashboardActionCard key={entry.to} entry={entry} />
        ))}
      </div>
    </section>
  )
}

/**
 * 单个仪表盘入口卡片，按配置渲染图标、说明和跳转按钮。
 */
function DashboardActionCard({ entry }: { entry: AppEntry }) {
  const { t } = useI18n()
  const Icon = entry.icon
  const theme = appModuleThemes[entry.tone]

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className={`flex size-12 items-center justify-center rounded-2xl ${theme.icon}`}>
            <Icon className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl text-slate-950">{t(entry.dashboardTitleKey)}</CardTitle>
            <CardDescription>{t(entry.dashboardDescriptionKey)}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <Button asChild className={`rounded-2xl ${theme.button}`}>
          <Link to={entry.to}>{t(entry.dashboardOpenKey)}</Link>
        </Button>
      </CardContent>
    </Card>
  )
}
