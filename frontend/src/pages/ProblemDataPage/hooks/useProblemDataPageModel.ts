import { useCallback, useEffect, useMemo, useReducer, useRef } from 'react'

import { ClearProblemData } from '@/apis/problem/ClearProblemData'
import { DeleteProblemDataPath } from '@/apis/problem/DeleteProblemDataPath'
import { DownloadProblemDataArchive } from '@/apis/problem/DownloadProblemDataArchive'
import { DownloadProblemDataPath } from '@/apis/problem/DownloadProblemDataPath'
import { ListProblemDataTree } from '@/apis/problem/ListProblemDataTree'
import { SetProblemDataReady } from '@/apis/problem/SetProblemDataReady'
import { UploadProblemDataArchive } from '@/apis/problem/UploadProblemDataArchive'
import { UploadProblemDataFile } from '@/apis/problem/UploadProblemDataFile'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { parseProblemDataFilename, problemDataFilenameValue } from '@/objects/problem/ProblemDataFilename'
import { problemDataPathValue } from '@/objects/problem/ProblemDataPath'
import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import {
  initialProblemDataPageState,
  reduceProblemDataPageState,
} from '../functions/ProblemDataPageState'
import { useProblemDetailQuery } from '@/pages/hooks/useProblemDetailQuery'
import { sendAPI, sendMultipartAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 文件上传操作结果；失败时 message 可直接展示给页面。
 */
type UploadResult = { ok: true } | { ok: false; message: string }
/**
 * 文件删除或清空操作结果；成功后调用方可继续刷新列表或关闭确认框。
 */
type DeleteResult = { ok: true } | { ok: false; message: string }
/**
 * 测试数据 ready 状态保存结果；失败时保留服务端或本地错误消息。
 */
type ReadyResult = { ok: true } | { ok: false; message: string }

/**
 * 题目测试数据页模型 hook，组合题目详情、文件树加载、上传下载、删除清空和 ready 状态保存。
 * 输入为题目 slug 与可选竞赛 slug，所有 API 副作用在 hook 内完成并通过 reducer 暴露 UI 状态。
 */
export function useProblemDataPageModel(problemSlug: ProblemSlug, contestSlug?: ContestSlug) {
  const { t } = useI18n()
  const detailQuery = useProblemDetailQuery(problemSlug, contestSlug)
  const problemDataScope = useMemo(() => ({ problemSlug, contestSlug }), [contestSlug, problemSlug])
  const problem = detailQuery.problem
  const replaceProblem = detailQuery.replaceProblem
  const [state, dispatch] = useReducer(reduceProblemDataPageState, initialProblemDataPageState)
  const loadFilesRequestRef = useRef(0)
  const uploadWarningMessage = (() => {
    const selectedFile = state.selectedFile
    if (!selectedFile) {
      return ''
    }

    const isZipArchive = selectedFile.name.toLowerCase().endsWith('.zip')
    if (isZipArchive) {
      const hasExistingFiles = state.dataTree.some((node) => node.kind === 'file')
      return hasExistingFiles ? t('problem.data.archiveOverwriteWarning') : ''
    }

    const overwritesExistingFile = state.dataTree.some(
      (node) => node.kind === 'file' && problemDataPathValue(node.path) === selectedFile.name,
    )
    return overwritesExistingFile
      ? t('problem.data.fileOverwriteWarning', { filename: selectedFile.name })
      : ''
  })()

  const loadFiles = useCallback(async () => {
    const requestId = loadFilesRequestRef.current + 1
    loadFilesRequestRef.current = requestId
    const isCurrentRequest = () => loadFilesRequestRef.current === requestId
    dispatch({ type: 'load_started' })
    try {
      const tree = await sendAPI(new ListProblemDataTree(problemDataScope.problemSlug, problemDataScope.contestSlug))
      if (!isCurrentRequest()) {
        return { ok: false as const, message: t('problem.data.loadFailed') }
      }
      dispatch({ type: 'load_succeeded', tree: tree.items })
      return { ok: true as const }
    } catch (error) {
      const message = isHttpClientError(error) ? error.message : t('problem.data.loadFailed')
      if (!isCurrentRequest()) {
        return { ok: false as const, message }
      }
      dispatch({ type: 'load_failed', message })
      return { ok: false as const, message }
    }
  }, [problemDataScope, t])

  useEffect(() => {
    void loadFiles()
    return () => {
      loadFilesRequestRef.current += 1
    }
  }, [loadFiles])

  const uploadSelectedFile = useCallback(async (): Promise<UploadResult> => {
    if (!state.selectedFile) {
      const message = 'Please choose a file to upload.'
      dispatch({ type: 'upload_failed', message })
      return { ok: false, message }
    }

    const filenameResult = parseProblemDataFilename(state.selectedFile.name)
    if (!filenameResult.ok) {
      dispatch({ type: 'upload_failed', message: filenameResult.error })
      return { ok: false, message: filenameResult.error }
    }

    dispatch({ type: 'upload_started' })

    try {
      const isZipArchive = state.selectedFile.name.toLowerCase().endsWith('.zip')
      const updatedProblem = await (() => {
        if (isZipArchive) {
          const api = new UploadProblemDataArchive(problemDataScope.problemSlug, state.selectedFile, problemDataScope.contestSlug)
          return sendMultipartAPI(api, api.formData())
        }

        const api = new UploadProblemDataFile(problemDataScope.problemSlug, state.selectedFile, filenameResult.value, problemDataScope.contestSlug)
        return sendMultipartAPI(api, api.formData())
      })()

      replaceProblem(updatedProblem.problem)
      dispatch({
        type: 'upload_succeeded',
        message: isZipArchive
          ? `Uploaded ${updatedProblem.uploadedFileCount} file(s) successfully.`
          : `Uploaded ${updatedProblem.problem.data.value ?? problemDataFilenameValue(filenameResult.value)} successfully.`,
      })
      await loadFiles()
      return { ok: true }
    } catch (error) {
      const message = isHttpClientError(error) ? error.message : 'Unable to upload problem data.'
      dispatch({ type: 'upload_failed', message })
      return { ok: false, message }
    }
  }, [loadFiles, problemDataScope, replaceProblem, state.selectedFile])

  const deleteDataPath = useCallback(
    async (path: ProblemDataPath): Promise<DeleteResult> => {
      dispatch({ type: 'delete_started', path })

      try {
        const updatedProblem = await sendAPI(new DeleteProblemDataPath(problemDataScope.problemSlug, path, problemDataScope.contestSlug))
        replaceProblem(updatedProblem)
        dispatch({
          type: 'delete_succeeded',
          message: `Deleted ${problemDataPathValue(path)} successfully.`,
        })
        await loadFiles()
        return { ok: true }
      } catch (error) {
        const message = isHttpClientError(error) ? error.message : 'Unable to delete problem data.'
        dispatch({ type: 'delete_failed', message })
        return { ok: false, message }
      }
    },
    [loadFiles, problemDataScope, replaceProblem],
  )

  const clearAllDataFiles = useCallback(async (): Promise<DeleteResult> => {
    dispatch({ type: 'clear_started' })

    try {
      const updatedProblem = await sendAPI(new ClearProblemData(problemDataScope.problemSlug, problemDataScope.contestSlug))
      replaceProblem(updatedProblem)
      dispatch({ type: 'clear_succeeded', message: 'Cleared all data files successfully.' })
      await loadFiles()
      return { ok: true }
    } catch (error) {
      const message = isHttpClientError(error) ? error.message : 'Unable to clear problem data.'
      dispatch({ type: 'clear_failed', message })
      return { ok: false, message }
    }
  }, [loadFiles, problemDataScope, replaceProblem])

  const setReady = useCallback(async (ready: boolean): Promise<ReadyResult> => {
    dispatch({ type: 'ready_save_started' })
    try {
      const updatedProblem = await sendAPI(new SetProblemDataReady(problemDataScope.problemSlug, ready, problemDataScope.contestSlug))
      replaceProblem(updatedProblem)
      dispatch({
        type: 'ready_save_succeeded',
        message: ready ? t('problem.data.ready.setSucceeded') : t('problem.data.ready.unsetSucceeded'),
      })
      await loadFiles()
      return { ok: true }
    } catch (error) {
      const message = isHttpClientError(error) ? error.message : t('problem.data.ready.saveFailed')
      dispatch({ type: 'ready_save_failed', message })
      return { ok: false, message }
    }
  }, [loadFiles, problemDataScope, replaceProblem, t])

  return {
    problem,
    isProblemLoading: detailQuery.isLoading,
    problemErrorMessage: detailQuery.errorMessage,
    isSavingReady: state.isSavingReady,
    selectedFile: state.selectedFile,
    isUploading: state.isUploading,
    isLoadingFiles: state.isLoadingFiles,
    deletingPath: state.deletingPath,
    isClearingAll: state.isClearingAll,
    dataTree: state.dataTree,
    errorMessage: state.errorMessage,
    successMessage: state.successMessage,
    uploadWarningMessage,
    setSelectedFile: (file: File | null) => dispatch({ type: 'selected_file_set', file }),
    setErrorMessage: (message: string) =>
      dispatch(message ? { type: 'load_failed', message } : { type: 'error_cleared' }),
    setSuccessMessage: (message: string) =>
      dispatch(message ? { type: 'upload_succeeded', message } : { type: 'success_cleared' }),
    replaceProblem,
    loadFiles,
    uploadSelectedFile,
    deleteDataPath,
    downloadDataPathUrl: (path: ProblemDataPath) =>
      new DownloadProblemDataPath(problemDataScope.problemSlug, path, problemDataScope.contestSlug).downloadUrl(),
    downloadDataArchiveUrl: () =>
      new DownloadProblemDataArchive(problemDataScope.problemSlug, problemDataScope.contestSlug).downloadUrl(),
    clearAllDataFiles,
    setReady,
  }
}
