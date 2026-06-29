import { Link } from 'react-router-dom'
import { Database, Files, MessageSquareText, PencilLine, ScrollText, Send, ShieldCheck } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { formatProblemTitleDisplay, shouldShowProblemSlugSupplement } from '@/pages/objects/ProblemTitleDisplay'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { problemStatementTextValue } from '@/objects/problem/ProblemStatementText'
import type { useProblemDetailPageModel } from '../hooks/useProblemDetailPageModel'
import { useProblemTitleDisplayMode } from '@/pages/hooks/useProblemTitleDisplay'
import { UserProfileLink } from '@/pages/components/UserProfileLink'
import { MarkdownDocument } from '@/pages/components/MarkdownDocument'
import { resourceAccessBadgeLabel } from '@/pages/objects/ResourceAccessDisplay'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 题目详情页模型类型别名，供头部卡片读取题目和操作状态。
 */
type ProblemDetailPageModel = ReturnType<typeof useProblemDetailPageModel>

/**
 * 题目详情头部卡片属性，包含模型、标题展示偏好和比赛上下文。
 */
type ProblemDetailHeaderCardProps = {
  canManageProblem: boolean
  managementPanel: 'edit' | 'access' | null
  model: ProblemDetailPageModel
  setManagementPanel: (panel: 'edit' | 'access' | null | ((currentPanel: 'edit' | 'access' | null) => 'edit' | 'access' | null)) => void
}

/**
 * 题目详情头部卡片，展示题目标题、访问状态和主要操作入口。
 */
export function ProblemDetailHeaderCard({
  canManageProblem,
  managementPanel,
  model,
  setManagementPanel,
}: ProblemDetailHeaderCardProps) {
  const { t } = useI18n()
  const problemTitleDisplayMode = useProblemTitleDisplayMode()

  if (!model.problem) {
    return null
  }

  const titleText = formatProblemTitleDisplay(model.problem.title, model.problem.slug, problemTitleDisplayMode)
  const problemSlugText = problemSlugValue(model.problem.slug)
  const contestProblemBasePath = model.contestSlug
    ? `/contests/${contestSlugValue(model.contestSlug)}/problems/${problemSlugText}`
    : `/problems/${problemSlugText}`

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
              <ScrollText className="size-5" />
          </div>
          <div>
              <CardTitle className="text-2xl text-slate-950">{titleText}</CardTitle>
              {shouldShowProblemSlugSupplement(problemTitleDisplayMode) ? (
                <CardDescription className="mt-2 font-mono text-sm text-slate-500">
                  {problemSlugValue(model.problem.slug)}
                </CardDescription>
              ) : null}
            </div>
          </div>

          <div className="flex flex-wrap gap-3">
            <Button
              asChild
              variant="outline"
              className="rounded-2xl border-emerald-300 bg-white text-emerald-900 hover:bg-emerald-50"
            >
              <Link to={`${contestProblemBasePath}/submit`}>
                <Send className="size-4" />
                {t('problem.detail.submitCode')}
              </Link>
            </Button>
            <Button
              asChild
              variant="outline"
              className="rounded-2xl border-emerald-300 bg-white text-emerald-900 hover:bg-emerald-50"
            >
              <Link to={`/problems/${problemSlugValue(model.problem.slug)}/submissions`}>
                <Files className="size-4" />
                {t('problem.detail.viewSubmissions')}
              </Link>
            </Button>
            <Button
              asChild
              variant="outline"
              className="rounded-2xl border-emerald-300 bg-white text-emerald-900 hover:bg-emerald-50"
            >
              <Link to={`/problems/${problemSlugValue(model.problem.slug)}/blogs`}>
                <MessageSquareText className="size-4" />
                {t('problem.detail.viewBlogs')}
              </Link>
            </Button>
            {canManageProblem ? (
              <>
                <Button
                  asChild
                  variant="outline"
                  className="rounded-2xl border-emerald-300 bg-white text-emerald-900 hover:bg-emerald-50"
                >
              <Link to={`${contestProblemBasePath}/data`}>
                    <Database className="size-4" />
                    {t('problem.detail.manageData')}
                  </Link>
                </Button>
                <Button
                  type="button"
                  variant={managementPanel === 'edit' ? 'default' : 'outline'}
                  className={
                    managementPanel === 'edit'
                      ? 'rounded-2xl bg-emerald-300 text-emerald-950 hover:bg-emerald-400'
                      : 'rounded-2xl border-emerald-300 bg-white text-emerald-900 hover:bg-emerald-50'
                  }
                  onClick={() => {
                    setManagementPanel((currentPanel) => (currentPanel === 'edit' ? null : 'edit'))
                  }}
                >
                  <PencilLine className="size-4" />
                  {t('problem.detail.edit')}
                </Button>
                <Button
                  type="button"
                  variant={managementPanel === 'access' ? 'default' : 'outline'}
                  className={
                    managementPanel === 'access'
                      ? 'rounded-2xl bg-emerald-300 text-emerald-950 hover:bg-emerald-400'
                      : 'rounded-2xl border-emerald-300 bg-white text-emerald-900 hover:bg-emerald-50'
                  }
                  onClick={() => {
                    setManagementPanel((currentPanel) => (currentPanel === 'access' ? null : 'access'))
                  }}
                >
                  <ShieldCheck className="size-4" />
                  {t('problem.detail.accessManagement')}
                </Button>
              </>
            ) : null}
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-5">
        <div className="flex flex-wrap items-center gap-3">
          <Badge variant="secondary">{resourceAccessBadgeLabel(model.problem.accessPolicy)}</Badge>
        </div>
        <div className="rounded-3xl border border-slate-200 bg-slate-50 px-6 py-6">
          <MarkdownDocument content={problemStatementTextValue(model.problem.statement)} />
        </div>
        {model.problem.author ? (
          <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
            <span>{t('common.authorLabel')} </span>
            <UserProfileLink className="inline-flex items-baseline gap-2 normal-case tracking-normal" showUsername user={model.problem.author} />
          </p>
        ) : null}
      </CardContent>
    </Card>
  )
}
