import { useCallback, useEffect, useMemo, useState } from 'react'

import { DownloadProblemDataPath } from '@/apis/problem/DownloadProblemDataPath'
import { UploadProblemDataFile } from '@/apis/problem/UploadProblemDataFile'
import {
  judgeConfigPath,
  judgeConfigTemplate,
  validateJudgeConfigYaml,
} from '../functions/ProblemJudgeConfig'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { parseProblemDataPath } from '@/objects/problem/ProblemDataPath'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { useProblemDataPageModel } from './useProblemDataPageModel'
import { sendMultipartAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

type ProblemDataPageModel = ReturnType<typeof useProblemDataPageModel>

const judgePath = parseProblemDataPath(judgeConfigPath)
if (!judgePath.ok) {
  throw new Error(judgePath.error)
}
const judgeDataPath = judgePath.value

export function useProblemJudgeConfigEditorModel(model: ProblemDataPageModel, problemSlug: ProblemSlug, contestSlug?: ContestSlug) {
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
      const api = new DownloadProblemDataPath(problemSlug, judgeDataPath, contestSlug)
      const response = await fetch(api.downloadUrl(), {
        credentials: 'same-origin',
      })

      if (!response.ok) {
        throw new HttpClientError(
          response.status === 404
            ? 'not-found'
            : response.status === 403
              ? 'forbidden'
              : response.status === 401
                ? 'unauthorized'
                : 'http',
          response.statusText || `Unable to read ${judgeConfigPath}.`,
        )
      }

      const loaded = await response.text()
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
  }, [contestSlug, problemSlug, t])

  useEffect(() => {
    void loadConfig()
  }, [loadConfig])

  const saveConfig = useCallback(async () => {
    setErrorMessage('')
    setStatusMessage('')

    setIsSaving(true)
    try {
      const api = new UploadProblemDataFile(
        problemSlug,
        new File([content], judgeConfigPath, { type: 'text/plain' }),
        judgeDataPath,
        contestSlug,
      )
      const result = await sendMultipartAPI(api, api.formData())
      model.replaceProblem(result.problem)
      await model.loadFiles()
      setLastSavedContent(content)
      setStatusMessage(t('problem.data.judgeConfig.saved'))
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : t('problem.data.judgeConfig.saveFailed'))
    } finally {
      setIsSaving(false)
    }
  }, [contestSlug, content, model, problemSlug, t])

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
