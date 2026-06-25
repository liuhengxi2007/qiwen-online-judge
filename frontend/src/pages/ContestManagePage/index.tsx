import { Navigate, useParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { parseContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { PageLoadingCard } from '@/pages/components/PageLoadingCard'
import { PageShell } from '@/pages/components/PageShell'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { useI18n } from '@/system/i18n/use-i18n'

import { ContestManageFormCard } from './components/ContestManageFormCard'
import { ContestManageProblemCard } from './components/ContestManageProblemCard'
import { useContestManagePageModel } from './hooks/useContestManagePageModel'

/**
 * 比赛管理页入口，校验 slug 路由参数和管理员会话后渲染管理内容。
 */
export function ContestManagePage() {
  const { t } = useI18n()
  usePageTitle(t('contest.manage.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const { slug } = useParams<{ slug: string }>()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const slugResult = parseContestSlug(slug ?? '')
  if (!slugResult.ok) {
    return <Navigate replace to="/contests" />
  }

  return <ContestManagePageContent contestSlug={slugResult.value} />
}

/**
 * 比赛管理页主体，已解析的 contestSlug 在这里进入页面模型并组合管理业务区域。
 */
function ContestManagePageContent({ contestSlug }: { contestSlug: ContestSlug }) {
  const { t } = useI18n()
  const model = useContestManagePageModel(contestSlug)

  return (
    <PageShell title={t('contest.manage.heading')} mainClassName="bg-[linear-gradient(180deg,#f0fdfa_0%,#ecfeff_48%,#f8fafc_100%)]">
      {model.isLoading ? (
        <PageLoadingCard message={t('contest.manage.loading')} />
      ) : model.loadErrorMessage ? (
        <Alert variant="destructive">
          <AlertDescription>{model.loadErrorMessage}</AlertDescription>
        </Alert>
      ) : model.contest && model.draft ? (
        model.contest.canManage ? (
          <div className="space-y-6">
            <ContestManageFormCard model={model} />
            <ContestManageProblemCard model={model} contest={model.contest} />
          </div>
        ) : (
          <Alert variant="warning">
            <AlertDescription>{t('contest.manage.permissionRequired')}</AlertDescription>
          </Alert>
        )
      ) : null}
    </PageShell>
  )
}
