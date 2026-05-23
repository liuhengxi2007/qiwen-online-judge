import { CheckCircle2, FileCode2, PauseCircle, RefreshCw, RotateCcw, Save, Wand2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { useProblemJudgeConfigEditorModel } from '@/features/problem/hooks/use-problem-judge-config-editor-model'
import { judgeConfigPath } from '@/features/problem/lib/problem-judge-config'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import type { useProblemDataPageModel } from '@/features/problem/hooks/use-problem-data-page-model'
import { useI18n } from '@/shared/i18n/use-i18n'

type ProblemDataPageModel = ReturnType<typeof useProblemDataPageModel>

type ProblemJudgeConfigEditorCardProps = {
  model: ProblemDataPageModel
  problemSlug: ProblemSlug
}

export function ProblemJudgeConfigEditorCard({ model, problemSlug }: ProblemJudgeConfigEditorCardProps) {
  const { t } = useI18n()
  const editor = useProblemJudgeConfigEditorModel(model, problemSlug)

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
              className="rounded-2xl border-slate-300 bg-white"
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
              className="rounded-2xl border-slate-300 bg-white"
              onClick={editor.resetTemplate}
            >
              <RotateCcw className="size-4" />
              {t('problem.data.judgeConfig.template')}
            </Button>
            <Button
              type="button"
              variant="outline"
              disabled={editor.isLoading || editor.isSaving}
              className="rounded-2xl border-slate-300 bg-white"
              onClick={editor.validateCurrentContent}
            >
              <Wand2 className="size-4" />
              {t('problem.data.judgeConfig.validate')}
            </Button>
            <Button
              type="button"
              disabled={editor.isSaving || editor.isLoading || !editor.isDirty}
              className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
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
                disabled={model.isSavingReady}
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
                className="rounded-2xl bg-emerald-700 text-white hover:bg-emerald-800"
                onClick={() => {
                  void model.setReady(true)
                }}
              >
                <CheckCircle2 className="size-4" />
                {model.isSavingReady ? t('problem.data.ready.saving') : t('problem.data.ready.setReady')}
              </Button>
            )}
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
            className="min-h-[28rem] resize-y rounded-2xl bg-slate-950 font-mono text-sm leading-6 text-slate-50 shadow-inner selection:bg-emerald-300 selection:text-slate-950"
            onChange={(event) => {
              editor.setContent(event.target.value)
            }}
          />
        </div>

        {!editor.validation.ok ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="space-y-1 text-rose-700">
              {editor.validation.errors.slice(0, 8).map((error) => (
                <p key={error}>{error}</p>
              ))}
              {editor.validation.errors.length > 8 ? <p>{t('problem.data.judgeConfig.moreErrors', { count: editor.validation.errors.length - 8 })}</p> : null}
            </AlertDescription>
          </Alert>
        ) : (
          <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
            <AlertDescription className="flex items-center gap-2 text-emerald-700">
              <CheckCircle2 className="size-4" />
              {t('problem.data.judgeConfig.valid')}
            </AlertDescription>
          </Alert>
        )}

        {editor.errorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{editor.errorMessage}</AlertDescription>
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
