import { useCallback, useEffect, useMemo, useRef, useState } from 'react'

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
import { createHttpClientError, isHttpClientError, readErrorMessage } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 测试数据页模型类型别名，供 judge 配置编辑器读取文件树、刷新列表和替换题目详情。
 */
type ProblemDataPageModel = ReturnType<typeof useProblemDataPageModel>

const judgePath = parseProblemDataPath(judgeConfigPath)
if (!judgePath.ok) {
  throw new Error(judgePath.error)
}
const judgeDataPath = judgePath.value

/**
 * judge.yaml 编辑器模型，负责加载现有配置、插入模板、实时校验 YAML 并保存为测试数据文件。
 * 加载使用浏览器 fetch 读取下载 URL；保存通过 multipart API 上传并刷新题目数据列表。
 */
export function useProblemJudgeConfigEditorModel(model: ProblemDataPageModel, problemSlug: ProblemSlug, contestSlug?: ContestSlug) {
  const { t } = useI18n()
  const problemDataScope = useMemo(() => ({ problemSlug, contestSlug }), [contestSlug, problemSlug])
  const [content, setContentValue] = useState(judgeConfigTemplate)
  const [lastSavedContent, setLastSavedContent] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [statusMessage, setStatusMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const loadConfigRequestRef = useRef(0)

  const validation = useMemo(() => validateJudgeConfigYaml(content, model.dataTree), [content, model.dataTree])
  const isDirty = lastSavedContent === null || content !== lastSavedContent

  const loadConfig = useCallback(async () => {
    const requestId = loadConfigRequestRef.current + 1
    loadConfigRequestRef.current = requestId
    const isCurrentRequest = () => loadConfigRequestRef.current === requestId
    setIsLoading(true)
    setErrorMessage('')
    setStatusMessage('')
    try {
      const api = new DownloadProblemDataPath(problemDataScope.problemSlug, judgeDataPath, problemDataScope.contestSlug)
      const response = await fetch(api.downloadUrl(), {
        credentials: 'same-origin',
      })

      if (!response.ok) {
        const message = await readErrorMessage(response, response.statusText || `Unable to read ${judgeConfigPath}.`)
        throw createHttpClientError(
          response.status === 404
            ? 'not-found'
            : response.status === 403
              ? 'forbidden'
              : response.status === 401
                ? 'unauthorized'
                : 'http',
          message,
        )
      }

      const loaded = await response.text()
      if (!isCurrentRequest()) {
        return
      }
      setContentValue(loaded)
      setLastSavedContent(loaded)
      setStatusMessage(t('problem.data.judgeConfig.loaded'))
    } catch (error) {
      if (!isCurrentRequest()) {
        return
      }
      if (isHttpClientError(error) && error.kind === 'not-found') {
        setContentValue(judgeConfigTemplate)
        setLastSavedContent(null)
        setStatusMessage(t('problem.data.judgeConfig.templateUnsaved'))
      } else {
        setErrorMessage(error instanceof Error ? error.message : t('problem.data.judgeConfig.loadFailed'))
      }
    } finally {
      if (isCurrentRequest()) {
        setIsLoading(false)
      }
    }
  }, [problemDataScope, t])

  useEffect(() => {
    void loadConfig()
    return () => {
      loadConfigRequestRef.current += 1
    }
  }, [loadConfig])

  const saveConfig = useCallback(async () => {
    setErrorMessage('')
    setStatusMessage('')

    setIsSaving(true)
    try {
      const api = new UploadProblemDataFile(
        problemDataScope.problemSlug,
        new File([content], judgeConfigPath, { type: 'text/plain' }),
        judgeDataPath,
        problemDataScope.contestSlug,
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
  }, [content, model, problemDataScope, t])

  const resetTemplate = useCallback(() => {
    setContentValue(judgeConfigTemplate)
    setStatusMessage(t('problem.data.judgeConfig.templateInserted'))
    setErrorMessage('')
  }, [t])

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
    setContent,
  }
}
