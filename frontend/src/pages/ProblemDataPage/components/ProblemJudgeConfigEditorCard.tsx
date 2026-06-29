import { CheckCircle2, FileCode2, PauseCircle, RefreshCw, RotateCcw, Save } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { ConfirmActionDialog } from '@/pages/components/ConfirmActionDialog'
import { useProblemJudgeConfigEditorModel } from '../hooks/useProblemJudgeConfigEditorModel'
import { judgeConfigPath } from '../functions/ProblemJudgeConfig'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { useProblemDataPageModel } from '../hooks/useProblemDataPageModel'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 测试数据页模型类型别名，供 judge 配置编辑卡片保存配置后刷新题目与文件树。
 */
type ProblemDataPageModel = ReturnType<typeof useProblemDataPageModel>

/**
 * judge 配置编辑卡片属性，包含题目作用域和测试数据页模型。
 */
type ProblemJudgeConfigEditorCardProps = {
  contestSlug?: ContestSlug
  model: ProblemDataPageModel
  problemSlug: ProblemSlug
}

/**
 * judge.yaml 在线编辑卡片，展示配置文本、校验结果、加载/保存动作以及 ready 状态切换按钮。
 * 保存与 ready 变更都委托给对应模型，组件只负责按钮状态和错误/成功消息呈现。
 */
export function ProblemJudgeConfigEditorCard({ contestSlug, model, problemSlug }: ProblemJudgeConfigEditorCardProps) {
  const { t } = useI18n()
  const editor = useProblemJudgeConfigEditorModel(model, problemSlug, contestSlug)

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardContent className="space-y-5 pt-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p className="flex items-center gap-2 text-sm font-medium text-slate-900">
              <FileCode2 className="size-4 text-slate-500" />
              {t('problem.data.judgeConfig.title')}
            </p>
            <p className="mt-1 text-sm text-slate-500">{t('problem.data.judgeConfig.description')}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button
              type="button"
              variant="outline"
              disabled={editor.isLoading || editor.isSaving}
              onClick={() => {
                void editor.loadConfig()
              }}
            >
              <RefreshCw className="size-4" />
              {t('problem.data.judgeConfig.reload')}
            </Button>
            <Button
              type="button"
              variant="outline"
              disabled={editor.isSaving}
              onClick={editor.resetTemplate}
            >
              <RotateCcw className="size-4" />
              {t('problem.data.judgeConfig.template')}
            </Button>
            <Button
              type="button"
              disabled={editor.isSaving || editor.isLoading || !editor.isDirty}
              onClick={() => {
                void editor.saveConfig()
              }}
            >
              <Save className="size-4" />
              {editor.isSaving ? t('problem.data.judgeConfig.saving') : t('problem.data.judgeConfig.save')}
            </Button>
            {model.problem?.ready ? (
              <Button
                type="button"
                variant="outline"
                disabled={model.isSavingReady || model.isRejudgingProblem}
                className="rounded-2xl border-amber-300 bg-white text-amber-800"
                onClick={() => {
                  void model.setReady(false)
                }}
              >
                <PauseCircle className="size-4" />
                {model.isSavingReady ? t('problem.data.ready.saving') : t('problem.data.ready.markNotReady')}
              </Button>
            ) : (
              <Button
                type="button"
                disabled={model.isSavingReady || editor.isLoading || editor.isDirty || !editor.validation.ok}
                variant="success"
                onClick={() => {
                  void model.setReady(true)
                }}
              >
                <CheckCircle2 className="size-4" />
                {model.isSavingReady ? t('problem.data.ready.saving') : t('problem.data.ready.setReady')}
              </Button>
            )}
            <ConfirmActionDialog
              title={t('problem.data.rejudgeAll.title')}
              description={t('problem.data.rejudgeAll.description')}
              confirmLabel={model.isRejudgingProblem ? t('problem.data.rejudgeAll.running') : t('problem.data.rejudgeAll.confirm')}
              onConfirm={() => {
                void model.rejudgeProblemSubmissions()
              }}
              trigger={
                <Button
                  type="button"
                  variant="outline"
                  disabled={model.isRejudgingProblem || model.isSavingReady || !model.problem?.ready}
                  className="rounded-2xl border-slate-300 bg-white"
                >
                  <RefreshCw className="size-4" />
                  {model.isRejudgingProblem ? t('problem.data.rejudgeAll.running') : t('problem.data.rejudgeAll.action')}
                </Button>
              }
            />
          </div>
        </div>

        <div className="space-y-2">
          <div className="flex items-center justify-between gap-3">
            <Label htmlFor="problem-judge-config">{judgeConfigPath}</Label>
            <span className="text-xs font-medium text-slate-500">
              {editor.isDirty ? t('problem.data.judgeConfig.unsaved') : t('problem.data.judgeConfig.savedState')}
            </span>
          </div>
          <Textarea
            id="problem-judge-config"
            spellCheck={false}
            value={editor.content}
            disabled={editor.isLoading}
            className="min-h-[28rem] resize-y rounded-2xl border-slate-300 bg-white font-mono text-sm leading-6 text-slate-950 shadow-inner selection:bg-emerald-200 selection:text-slate-950 focus-visible:ring-emerald-500"
            onChange={(event) => {
              editor.setContent(event.target.value)
            }}
          />
        </div>

        {!editor.validation.ok ? (
          <Alert variant="destructive">
            <AlertDescription className="space-y-1">
              {editor.validation.errors.slice(0, 8).map((error) => (
                <p key={error}>{error}</p>
              ))}
              {editor.validation.errors.length > 8 ? <p>{t('problem.data.judgeConfig.moreErrors', { count: editor.validation.errors.length - 8 })}</p> : null}
            </AlertDescription>
          </Alert>
        ) : (
          <Alert variant="success">
            <AlertDescription className="flex items-center gap-2">
              <CheckCircle2 className="size-4" />
              {t('problem.data.judgeConfig.valid')}
            </AlertDescription>
          </Alert>
        )}

        {editor.errorMessage ? (
          <Alert variant="destructive">
            <AlertDescription>{editor.errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {editor.statusMessage ? (
          <Alert className="rounded-2xl border-sky-200 bg-sky-50/95">
            <AlertDescription className="text-sky-800">{editor.statusMessage}</AlertDescription>
          </Alert>
        ) : null}
      </CardContent>
    </Card>
  )
}
