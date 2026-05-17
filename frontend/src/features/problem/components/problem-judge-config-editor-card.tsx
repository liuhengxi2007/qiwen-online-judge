import { useCallback, useEffect, useMemo, useState } from 'react'
<<<<<<< HEAD
import { CheckCircle2, FileCode2, PauseCircle, RefreshCw, RotateCcw, Save, Wand2 } from 'lucide-react'
=======
import { CheckCircle2, FileCode2, RefreshCw, RotateCcw, Save, Wand2 } from 'lucide-react'
>>>>>>> origin/main

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  readProblemDataText,
  saveProblemDataText,
} from '@/features/problem/api/problem-client'
import {
  judgeConfigPath,
  judgeConfigTemplate,
  validateJudgeConfigYaml,
} from '@/features/problem/domain/problem-judge-config'
import { parseProblemDataPath, type ProblemSlug } from '@/features/problem/domain/problem'
import type { useProblemDataPageModel } from '@/features/problem/hooks/use-problem-data-page-model'
import { HttpClientError } from '@/shared/api/http-client'
<<<<<<< HEAD
import { useI18n } from '@/shared/i18n/use-i18n'
=======
import { useI18n } from '@/shared/i18n/i18n'
>>>>>>> origin/main

type ProblemDataPageModel = ReturnType<typeof useProblemDataPageModel>

type ProblemJudgeConfigEditorCardProps = {
  model: ProblemDataPageModel
  problemSlug: ProblemSlug
}

const judgePath = parseProblemDataPath(judgeConfigPath)
if (!judgePath.ok) {
  throw new Error(judgePath.error)
}
const judgeDataPath = judgePath.value

export function ProblemJudgeConfigEditorCard({ model, problemSlug }: ProblemJudgeConfigEditorCardProps) {
  const { t } = useI18n()
  const [content, setContent] = useState(judgeConfigTemplate)
  const [lastSavedContent, setLastSavedContent] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [statusMessage, setStatusMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  const validation = useMemo(() => validateJudgeConfigYaml(content, model.dataTree), [content, model.dataTree])
  const isDirty = lastSavedContent === null || content !== lastSavedContent

  const loadConfig = useCallback(async () => {
    setIsLoading(true)
    setErrorMessage('')
    setStatusMessage('')
    try {
      const loaded = await readProblemDataText(problemSlug, judgeDataPath)
      setContent(loaded)
      setLastSavedContent(loaded)
      setStatusMessage(t('problem.data.judgeConfig.loaded'))
    } catch (error) {
      if (error instanceof HttpClientError && error.kind === 'not-found') {
        setContent(judgeConfigTemplate)
        setLastSavedContent(null)
        setStatusMessage(t('problem.data.judgeConfig.templateUnsaved'))
      } else {
        setErrorMessage(error instanceof Error ? error.message : t('problem.data.judgeConfig.loadFailed'))
      }
    } finally {
      setIsLoading(false)
    }
  }, [problemSlug, t])

  useEffect(() => {
    void loadConfig()
  }, [loadConfig])

  const saveConfig = async () => {
    setErrorMessage('')
    setStatusMessage('')

<<<<<<< HEAD
=======
    if (!validation.ok) {
      setErrorMessage(t('problem.data.judgeConfig.validationBlocksSave'))
      return
    }

>>>>>>> origin/main
    setIsSaving(true)
    try {
      const result = await saveProblemDataText(problemSlug, judgeDataPath, content)
      model.replaceProblem(result.problem)
      await model.loadFiles()
      setLastSavedContent(content)
      setStatusMessage(t('problem.data.judgeConfig.saved'))
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : t('problem.data.judgeConfig.saveFailed'))
    } finally {
      setIsSaving(false)
    }
  }

  const resetTemplate = () => {
    setContent(judgeConfigTemplate)
    setStatusMessage(t('problem.data.judgeConfig.templateInserted'))
    setErrorMessage('')
  }

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
              disabled={isLoading || isSaving}
              className="rounded-2xl border-slate-300 bg-white"
              onClick={() => {
                void loadConfig()
              }}
            >
              <RefreshCw className="size-4" />
              {t('problem.data.judgeConfig.reload')}
            </Button>
            <Button
              type="button"
              variant="outline"
              disabled={isSaving}
              className="rounded-2xl border-slate-300 bg-white"
              onClick={resetTemplate}
            >
              <RotateCcw className="size-4" />
              {t('problem.data.judgeConfig.template')}
            </Button>
            <Button
              type="button"
              variant="outline"
              disabled={isLoading || isSaving}
              className="rounded-2xl border-slate-300 bg-white"
              onClick={() => {
                setErrorMessage('')
                setStatusMessage(
                  validation.ok
                    ? t('problem.data.judgeConfig.valid')
                    : t('problem.data.judgeConfig.invalid'),
                )
              }}
            >
              <Wand2 className="size-4" />
              {t('problem.data.judgeConfig.validate')}
            </Button>
            <Button
              type="button"
<<<<<<< HEAD
              disabled={isSaving || isLoading || !isDirty}
=======
              disabled={isSaving || isLoading || !isDirty || !validation.ok}
>>>>>>> origin/main
              className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
              onClick={() => {
                void saveConfig()
              }}
            >
              <Save className="size-4" />
              {isSaving ? t('problem.data.judgeConfig.saving') : t('problem.data.judgeConfig.save')}
            </Button>
<<<<<<< HEAD
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
                disabled={model.isSavingReady || isLoading || isDirty || !validation.ok}
                className="rounded-2xl bg-emerald-700 text-white hover:bg-emerald-800"
                onClick={() => {
                  void model.setReady(true)
                }}
              >
                <CheckCircle2 className="size-4" />
                {model.isSavingReady ? t('problem.data.ready.saving') : t('problem.data.ready.setReady')}
              </Button>
            )}
=======
>>>>>>> origin/main
          </div>
        </div>

        <div className="space-y-2">
          <div className="flex items-center justify-between gap-3">
            <Label htmlFor="problem-judge-config">{judgeConfigPath}</Label>
            <span className="text-xs font-medium text-slate-500">
              {isDirty ? t('problem.data.judgeConfig.unsaved') : t('problem.data.judgeConfig.savedState')}
            </span>
          </div>
          <Textarea
            id="problem-judge-config"
            spellCheck={false}
            value={content}
            disabled={isLoading}
            className="min-h-[28rem] resize-y rounded-2xl bg-slate-950 font-mono text-sm leading-6 text-slate-50 shadow-inner selection:bg-emerald-300 selection:text-slate-950"
            onChange={(event) => {
              setContent(event.target.value)
              setErrorMessage('')
              setStatusMessage('')
            }}
          />
        </div>

        {!validation.ok ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="space-y-1 text-rose-700">
              {validation.errors.slice(0, 8).map((error) => (
                <p key={error}>{error}</p>
              ))}
              {validation.errors.length > 8 ? <p>{t('problem.data.judgeConfig.moreErrors', { count: validation.errors.length - 8 })}</p> : null}
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

        {errorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {statusMessage ? (
          <Alert className="rounded-2xl border-sky-200 bg-sky-50/95">
            <AlertDescription className="text-sky-800">{statusMessage}</AlertDescription>
          </Alert>
        ) : null}
      </CardContent>
    </Card>
  )
}
