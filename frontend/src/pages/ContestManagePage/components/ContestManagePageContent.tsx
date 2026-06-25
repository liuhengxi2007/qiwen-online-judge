import { Alert, AlertDescription } from '@/components/ui/alert'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { PageLoadingCard } from '@/pages/components/PageLoadingCard'
import { PageShell } from '@/pages/components/PageShell'
import { useI18n } from '@/system/i18n/use-i18n'

import { ContestManageFormCard } from './ContestManageFormCard'
import { ContestManageProblemCard } from './ContestManageProblemCard'
import { useContestManagePageModel } from '../hooks/useContestManagePageModel'

/**
 * 保留内容组件 props 解构：这里是路由 slug 进入比赛管理模型的边界。
 */
export function ContestManagePageContent({ contestSlug }: { contestSlug: ContestSlug }) {
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
