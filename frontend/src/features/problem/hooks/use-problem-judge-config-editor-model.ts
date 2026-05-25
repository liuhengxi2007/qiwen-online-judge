import { useCallback, useEffect, useMemo, useState } from 'react'

import { readProblemDataText } from '@/features/problem/http/api/DownloadProblemDataPath'
import { saveProblemDataText } from '@/features/problem/http/api/UploadProblemDataFile'
import {
  judgeConfigPath,
  judgeConfigTemplate,
  validateJudgeConfigYaml,
} from '@/features/problem/lib/problem-judge-config'
import { parseProblemDataPath } from '@/features/problem/lib/problem-parsers'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import type { useProblemDataPageModel } from '@/features/problem/hooks/use-problem-data-page-model'
import { HttpClientError } from '@/shared/api/http-client'
import { useI18n } from '@/shared/i18n/use-i18n'

type ProblemDataPageModel = ReturnType<typeof useProblemDataPageModel>

const judgePath = parseProblemDataPath(judgeConfigPath)
if (!judgePath.ok) {
  throw new Error(judgePath.error)
}
const judgeDataPath = judgePath.value

export function useProblemJudgeConfigEditorModel(model: ProblemDataPageModel, problemSlug: ProblemSlug) {
  const { t } = useI18n()
  const [content, setContentValue] = useState(judgeConfigTemplate)
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
      setContentValue(loaded)
      setLastSavedContent(loaded)
      setStatusMessage(t('problem.data.judgeConfig.loaded'))
    } catch (error) {
      if (error instanceof HttpClientError && error.kind === 'not-found') {
        setContentValue(judgeConfigTemplate)
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

  const saveConfig = useCallback(async () => {
    setErrorMessage('')
    setStatusMessage('')

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
  }, [content, model, problemSlug, t])

  const resetTemplate = useCallback(() => {
    setContentValue(judgeConfigTemplate)
    setStatusMessage(t('problem.data.judgeConfig.templateInserted'))
    setErrorMessage('')
  }, [t])

  const validateCurrentContent = useCallback(() => {
    setErrorMessage('')
    setStatusMessage(
      validation.ok
        ? t('problem.data.judgeConfig.valid')
        : t('problem.data.judgeConfig.invalid'),
    )
  }, [t, validation.ok])

  const setContent = useCallback((value: string) => {
    setContentValue(value)
    setErrorMessage('')
    setStatusMessage('')
  }, [])

  return {
    content,
    isLoading,
    isSaving,
    statusMessage,
    errorMessage,
    validation,
    isDirty,
    loadConfig,
    saveConfig,
    resetTemplate,
    validateCurrentContent,
    setContent,
  }
}
