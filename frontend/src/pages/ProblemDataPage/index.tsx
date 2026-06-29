import { Navigate, useParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { ProblemDataFilesCard } from './components/ProblemDataFilesCard'
import { ProblemDataHeaderCard } from './components/ProblemDataHeaderCard'
import { ProblemDataUploadCard } from './components/ProblemDataUploadCard'
import { ProblemJudgeConfigEditorCard } from './components/ProblemJudgeConfigEditorCard'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { contestSlugValue, parseContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { parseProblemSlug, problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { useProblemDataPageModel } from './hooks/useProblemDataPageModel'
import { PageLoadingCard } from '@/pages/components/PageLoadingCard'
import { PageShell } from '@/pages/components/PageShell'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 题目测试数据管理入口页，负责登录保护、路由参数解析和竞赛题目作用域归一化。
 * 未登录用户跳转登录页，非法参数回到对应列表页，实际管理权限在内容组件中依据题目详情判断。
 */
export function ProblemDataPage() {
  const { t } = useI18n()
  usePageTitle(t('problem.data.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const { contestSlug, slug } = useParams<{ contestSlug?: string; slug: string }>()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const slugResult = parseProblemSlug(slug ?? '')
  if (!slugResult.ok) {
    return <Navigate replace to="/problems" />
  }

  const contestSlugResult = contestSlug ? parseContestSlug(contestSlug) : null
  if (contestSlugResult && !contestSlugResult.ok) {
    return <Navigate replace to="/contests" />
  }

  return <ProblemDataPageContent contestSlug={contestSlugResult?.ok ? contestSlugResult.value : undefined} problemSlug={slugResult.value} />
}

/**
 * 题目测试数据管理内容区，加载题目详情和数据文件，并在无管理权限时回到题目详情页。
 */
function ProblemDataPageContent({ contestSlug, problemSlug }: { contestSlug?: ContestSlug; problemSlug: ProblemSlug }) {
  const { t } = useI18n()
  const model = useProblemDataPageModel(problemSlug, contestSlug)

  if (!model.isProblemLoading && model.problem && !model.problem.canManage) {
    return (
      <Navigate
        replace
        to={contestSlug
          ? `/contests/${contestSlugValue(contestSlug)}/problems/${problemSlugValue(problemSlug)}`
          : `/problems/${problemSlugValue(problemSlug)}`}
      />
    )
  }

  return (
    <PageShell title={t('problem.data.heading')} mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#edf5f1_100%)]">
      {model.isProblemLoading ? (
        <PageLoadingCard message={t('problem.data.loading')} />
      ) : model.problem ? (
        <div className="space-y-6">
          <ProblemDataHeaderCard model={model} />
          <ProblemDataUploadCard model={model} />
          <ProblemJudgeConfigEditorCard contestSlug={contestSlug} model={model} problemSlug={problemSlug} />
          <ProblemDataFilesCard model={model} />
        </div>
      ) : (
        <Alert variant="destructive">
          <AlertDescription>
            {model.problemErrorMessage || t('problem.data.loadFailed')}
          </AlertDescription>
        </Alert>
      )}
    </PageShell>
  )
}
