import { Navigate } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { PageShell } from '@/pages/components/PageShell'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { useI18n } from '@/system/i18n/use-i18n'
import { useSubmissionPageModel } from './hooks/useSubmissionPageModel'
import { SubmissionFilterCard } from './SubmissionFilterCard'
import { SubmissionSummaryList } from './SubmissionSummaryList'

/**
 * 提交列表页面内容属性，允许固定题目筛选或限定在比赛范围内查询。
 */
type SubmissionListPageContentProps = {
  fixedProblemSlugFilter?: ProblemSlug
  contestSlug?: ContestSlug
  titleKey?: string
}

/**
 * 提交列表页面主体，负责会话守卫、筛选模型、错误提示、筛选卡片和结果列表组合。
 */
export function SubmissionListPageContent({ fixedProblemSlugFilter, contestSlug, titleKey = 'submission.heading' }: SubmissionListPageContentProps = {}) {
  const { t } = useI18n()
  usePageTitle(t('submission.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const model = useSubmissionPageModel(fixedProblemSlugFilter, contestSlug)

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  return (
    <PageShell title={t(titleKey)} mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#edf4fb_100%)]">
      {model.submissionQuery.errorMessage ? (
        <Alert variant="destructive" className="mb-6">
          <AlertDescription>{model.submissionQuery.errorMessage}</AlertDescription>
        </Alert>
      ) : null}

      <SubmissionFilterCard model={model} />

      <SubmissionSummaryList
        submissions={model.currentPageSubmissions}
        viewer={user}
        isLoading={model.submissionQuery.isLoading}
        currentPage={model.currentPage}
        totalPages={model.totalPages}
        onPageChange={model.goToPage}
      />
    </PageShell>
  )
}
