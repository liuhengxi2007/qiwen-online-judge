import { Link, Navigate, useParams } from 'react-router-dom'
import { Database, PencilLine, Settings2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { RowAction } from '@/components/ui/row-action'
import { contestProblemAliasValue } from '@/objects/contest/ContestProblemAlias'
import { contestSlugValue, parseContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { problemTitleValue } from '@/objects/problem/ProblemTitle'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'
import type { ProblemSuggestion } from '@/objects/problem/response/ProblemSuggestion'
import { MarkdownEditorTabs } from '@/pages/components/MarkdownEditorTabs'
import { PageLoadingCard } from '@/pages/components/PageLoadingCard'
import { PageShell } from '@/pages/components/PageShell'
import { ConfirmActionDialog } from '@/pages/components/ConfirmActionDialog'
import { ResourceAccessEditor } from '@/pages/components/ResourceAccessEditor'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { useI18n } from '@/system/i18n/use-i18n'
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
 * 比赛管理页主体，组合会话守卫、比赛管理模型、表单和题目关联工作流。
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

/**
 * 比赛基础信息表单卡片，负责展示草稿字段和保存比赛更新。
 */
function ContestManageFormCard({ model }: { model: ReturnType<typeof useContestManagePageModel> }) {
  const { t } = useI18n()
  const draft = model.draft
  if (!draft) {
    return null
  }

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
            <Settings2 className="size-5" />
          </div>
          <CardTitle className="text-xl text-slate-950">{t('contest.manage.basicInfo')}</CardTitle>
        </div>
      </CardHeader>
      <CardContent className="space-y-5">
        <div className="space-y-2">
          <Label htmlFor="contest-title">{t('contest.create.titleLabel')}</Label>
          <Input id="contest-title" value={draft.title} onChange={(event) => model.setTitle(event.target.value)} />
        </div>

        <div className="grid gap-5 md:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="contest-start-at">{t('contest.create.startAt')}</Label>
            <Input id="contest-start-at" type="datetime-local" value={draft.startAt} onChange={(event) => model.setStartAt(event.target.value)} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="contest-end-at">{t('contest.create.endAt')}</Label>
            <Input id="contest-end-at" type="datetime-local" value={draft.endAt} onChange={(event) => model.setEndAt(event.target.value)} />
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="contest-description">{t('contest.create.descriptionLabel')}</Label>
          <MarkdownEditorTabs
            textareaId="contest-description"
            value={draft.description}
            tab={model.descriptionTab}
            onTabChange={model.setDescriptionTab}
            onValueChange={model.setDescription}
            textareaClassName="min-h-48 font-mono"
          />
        </div>

        <ResourceAccessEditor
          accessPolicy={model.accessPolicy}
          grantedUsersInput={draft.grantedUsersInput}
          grantedGroupsInput={draft.grantedGroupsInput}
          grantedManagerUsersInput={draft.grantedManagerUsersInput}
          grantedManagerGroupsInput={draft.grantedManagerGroupsInput}
          onBaseAccessChange={model.setBaseAccess}
          onGrantedUsersInputChange={model.setGrantedUsersInput}
          onGrantedGroupsInputChange={model.setGrantedGroupsInput}
          onGrantedManagerUsersInputChange={model.setGrantedManagerUsersInput}
          onGrantedManagerGroupsInputChange={model.setGrantedManagerGroupsInput}
        />

        {model.saveErrorMessage ? (
          <Alert variant="destructive">
            <AlertDescription>{model.saveErrorMessage}</AlertDescription>
          </Alert>
        ) : null}
        {model.saveSuccessMessage ? (
          <Alert variant="success">
            <AlertDescription>{model.saveSuccessMessage}</AlertDescription>
          </Alert>
        ) : null}

        <div className="flex flex-wrap gap-3">
          <Button
            type="button"
            disabled={model.isSaving}
            onClick={() => {
              void model.save()
            }}
          >
            {model.isSaving ? t('contest.manage.saving') : t('contest.manage.save')}
          </Button>
          {model.contest ? (
            <Button asChild type="button" variant="outline" className="rounded-2xl">
              <Link to={`/contests/${contestSlugValue(model.contest.slug)}`}>{t('contest.manage.backToContest')}</Link>
            </Button>
          ) : null}
        </div>
      </CardContent>
    </Card>
  )
}

/**
 * 比赛题目管理卡片，展示已关联题目并提供附加、移除和排序入口。
 */
function ContestManageProblemCard({
  model,
  contest,
}: {
  model: ReturnType<typeof useContestManagePageModel>
  contest: ContestDetail
}) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <CardTitle className="text-xl text-slate-950">{t('contest.manage.problems')}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-5">
        {model.problemErrorMessage ? (
          <Alert variant="destructive">
            <AlertDescription>{model.problemErrorMessage}</AlertDescription>
          </Alert>
        ) : null}
        {model.problemSuccessMessage ? (
          <Alert variant="success">
            <AlertDescription>{model.problemSuccessMessage}</AlertDescription>
          </Alert>
        ) : null}

        <ProblemAttachInput
          input={model.problemSearchInput}
          isLoading={model.isLoadingProblemSuggestions}
          suggestions={model.problemSuggestions}
          isAttaching={model.isAttachingProblem}
          onInputChange={model.setProblemSearchInput}
          onFocusChange={model.setIsProblemSearchFocused}
          onSuggestionSelect={model.selectProblemSuggestion}
          onAttach={() => {
            void model.attachProblem()
          }}
        />
        <ConfirmActionDialog
          open={model.isAttachWarningOpen}
          onOpenChange={model.closeAttachProblemWarning}
          title={t('contest.manage.attachWarningTitle')}
          description={t('contest.manage.attachWarningDescription')}
          confirmLabel={t('contest.manage.attachWarningConfirm')}
          cancelLabel={t('contest.manage.attachWarningCancel')}
          onConfirm={() => {
            void model.confirmAttachProblemWarning()
          }}
        />

        <div className="space-y-3">
          {contest.problems.length === 0 ? (
            <p className="text-sm text-slate-500">{t('contest.detail.emptyProblems')}</p>
          ) : (
            contest.problems.map((problem) => (
              <div key={problem.id} className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-slate-50 p-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex flex-wrap items-center gap-2">
                  <Badge variant="outline">{contestProblemAliasValue(problem.alias)}</Badge>
                  <span className="text-sm font-medium text-slate-900">{problemTitleValue(problem.title)}</span>
                  <span className="text-sm text-slate-500">{problemSlugValue(problem.slug)}</span>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button asChild type="button" variant="outline" className="rounded-2xl border-emerald-200 bg-white text-emerald-800 hover:bg-emerald-50">
                    <Link to={`/contests/${contestSlugValue(contest.slug)}/problems/${problemSlugValue(problem.slug)}`}>
                      <PencilLine className="size-4" />
                      {t('problem.detail.edit')}
                    </Link>
                  </Button>
                  <Button asChild type="button" variant="outline" className="rounded-2xl border-slate-200 bg-white text-slate-800 hover:bg-slate-50">
                    <Link to={`/contests/${contestSlugValue(contest.slug)}/problems/${problemSlugValue(problem.slug)}/data`}>
                      <Database className="size-4" />
                      {t('problem.detail.manageData')}
                    </Link>
                  </Button>
                  <Button
                    type="button"
                    variant="destructiveOutline"
                    disabled={model.removingProblemSlug === problemSlugValue(problem.slug)}
                    onClick={() => {
                      void model.removeProblem(problemSlugValue(problem.slug))
                    }}
                  >
                    {model.removingProblemSlug === problemSlugValue(problem.slug) ? t('contest.manage.removingProblem') : t('contest.manage.removeProblem')}
                  </Button>
                </div>
              </div>
            ))
          )}
        </div>
      </CardContent>
    </Card>
  )
}

/**
 * 比赛题目附加输入组件，负责输入题目 slug 并触发附加操作。
 */
function ProblemAttachInput({
  input,
  isLoading,
  suggestions,
  isAttaching,
  onInputChange,
  onFocusChange,
  onSuggestionSelect,
  onAttach,
}: {
  input: string
  isLoading: boolean
  suggestions: ProblemSuggestion[]
  isAttaching: boolean
  onInputChange: (value: string) => void
  onFocusChange: (focused: boolean) => void
  onSuggestionSelect: (suggestion: ProblemSuggestion) => void
  onAttach: () => void
}) {
  const { t } = useI18n()

  return (
    <div className="space-y-2">
      <Label htmlFor="contest-problem-search">{t('contest.detail.attachProblemInput')}</Label>
      <Input
        id="contest-problem-search"
        value={input}
        onChange={(event) => onInputChange(event.target.value)}
        onFocus={() => onFocusChange(true)}
      />
      {suggestions.length > 0 || isLoading ? (
        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-2">
          {isLoading ? (
            <p className="px-3 py-2 text-sm text-slate-500">{t('common.loading')}</p>
          ) : (
            suggestions.map((suggestion) => (
              <RowAction
                key={problemSlugValue(suggestion.slug)}
                size="compact"
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => onSuggestionSelect(suggestion)}
              >
                <span className="font-medium text-slate-900">{problemTitleValue(suggestion.title)}</span>
                <span className="text-slate-500">{problemSlugValue(suggestion.slug)}</span>
              </RowAction>
            ))
          )}
        </div>
      ) : null}
      <Button type="button" variant="create" disabled={isAttaching} onClick={onAttach}>
        {isAttaching ? t('contest.detail.attachingProblem') : t('contest.detail.attachProblem')}
      </Button>
    </div>
  )
}
