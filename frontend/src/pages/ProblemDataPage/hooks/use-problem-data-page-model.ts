import { useCallback, useEffect, useReducer } from 'react'

import { ClearProblemData } from '@/apis/problem/ClearProblemData'
import { DeleteProblemData } from '@/apis/problem/DeleteProblemData'
import { DeleteProblemDataPath } from '@/apis/problem/DeleteProblemDataPath'
import { DownloadProblemDataPath } from '@/apis/problem/DownloadProblemDataPath'
import { ListProblemDataFiles } from '@/apis/problem/ListProblemDataFiles'
import { ListProblemDataTree } from '@/apis/problem/ListProblemDataTree'
import { SetProblemDataReady } from '@/apis/problem/SetProblemDataReady'
import { UploadProblemDataArchive } from '@/apis/problem/UploadProblemDataArchive'
import { UploadProblemDataFile } from '@/apis/problem/UploadProblemDataFile'
import { parseProblemDataFilename, problemDataFilenameValue } from '@/objects/problem/ProblemDataFilename'
import { problemDataPathValue } from '@/objects/problem/ProblemDataPath'
import type { ProblemDataFilename } from '@/objects/problem/ProblemDataFilename'
import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import {
  initialProblemDataPageState,
  reduceProblemDataPageState,
} from '../functions/problem-data-page-state'
import { useProblemDetailQuery } from '@/pages/hooks/use-problem-detail-query'
import { sendAPI, sendMultipartAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

type UploadResult = { ok: true } | { ok: false; message: string }
type DeleteResult = { ok: true } | { ok: false; message: string }
type ReadyResult = { ok: true } | { ok: false; message: string }

export function useProblemDataPageModel(problemSlug: ProblemSlug) {
  const { t } = useI18n()
  const detailQuery = useProblemDetailQuery(problemSlug)
  const problem = detailQuery.problem
  const replaceProblem = detailQuery.replaceProblem
  const [state, dispatch] = useReducer(reduceProblemDataPageState, initialProblemDataPageState)
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
    dispatch({ type: 'load_started' })
    try {
      const [files, tree] = await Promise.all([
        sendAPI(new ListProblemDataFiles(problemSlug)),
        sendAPI(new ListProblemDataTree(problemSlug)),
      ])
      dispatch({ type: 'load_succeeded', files: files.items, tree: tree.items })
      return { ok: true as const }
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('problem.data.loadFailed')
      dispatch({ type: 'load_failed', message })
      return { ok: false as const, message }
    }
  }, [problemSlug, t])

  useEffect(() => {
    void loadFiles()
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
          const api = new UploadProblemDataArchive(problemSlug, state.selectedFile)
          return sendMultipartAPI(api, api.formData())
        }

        const api = new UploadProblemDataFile(problemSlug, state.selectedFile, filenameResult.value)
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
      const message = error instanceof HttpClientError ? error.message : 'Unable to upload problem data.'
      dispatch({ type: 'upload_failed', message })
      return { ok: false, message }
    }
  }, [loadFiles, problemSlug, replaceProblem, state.selectedFile])

  const deleteDataFile = useCallback(
    async (filename: ProblemDataFilename): Promise<DeleteResult> => {
      dispatch({ type: 'delete_started', filename })

      try {
        const updatedProblem = await sendAPI(new DeleteProblemData(problemSlug, filename))
        replaceProblem(updatedProblem)
        dispatch({
          type: 'delete_succeeded',
          message: `Deleted ${problemDataFilenameValue(filename)} successfully.`,
        })
        await loadFiles()
        return { ok: true }
      } catch (error) {
        const message = error instanceof HttpClientError ? error.message : 'Unable to delete problem data.'
        dispatch({ type: 'delete_failed', message })
        return { ok: false, message }
      }
    },
    [loadFiles, problemSlug, replaceProblem],
  )

  const deleteDataPath = useCallback(
    async (path: ProblemDataPath): Promise<DeleteResult> => {
      dispatch({ type: 'delete_started', filename: path.split('/').slice(-1)[0] as ProblemDataFilename })

      try {
        const updatedProblem = await sendAPI(new DeleteProblemDataPath(problemSlug, path))
        replaceProblem(updatedProblem)
        dispatch({
          type: 'delete_succeeded',
          message: `Deleted ${problemDataPathValue(path)} successfully.`,
        })
        await loadFiles()
        return { ok: true }
      } catch (error) {
        const message = error instanceof HttpClientError ? error.message : 'Unable to delete problem data.'
        dispatch({ type: 'delete_failed', message })
        return { ok: false, message }
      }
    },
    [loadFiles, problemSlug, replaceProblem],
  )

  const clearAllDataFiles = useCallback(async (): Promise<DeleteResult> => {
    dispatch({ type: 'clear_started' })

    try {
      const updatedProblem = await sendAPI(new ClearProblemData(problemSlug))
      replaceProblem(updatedProblem)
      dispatch({ type: 'clear_succeeded', message: 'Cleared all data files successfully.' })
      await loadFiles()
      return { ok: true }
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : 'Unable to clear problem data.'
      dispatch({ type: 'clear_failed', message })
      return { ok: false, message }
    }
  }, [loadFiles, problemSlug, replaceProblem])

  const setReady = useCallback(async (ready: boolean): Promise<ReadyResult> => {
    dispatch({ type: 'ready_save_started' })
    try {
      const updatedProblem = await sendAPI(new SetProblemDataReady(problemSlug, ready))
      replaceProblem(updatedProblem)
      dispatch({
        type: 'ready_save_succeeded',
        message: ready ? t('problem.data.ready.setSucceeded') : t('problem.data.ready.unsetSucceeded'),
      })
      await loadFiles()
      return { ok: true }
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('problem.data.ready.saveFailed')
      dispatch({ type: 'ready_save_failed', message })
      return { ok: false, message }
    }
  }, [loadFiles, problemSlug, replaceProblem, t])

  return {
    problem,
    isProblemLoading: detailQuery.isLoading,
    problemErrorMessage: detailQuery.errorMessage,
    isSavingReady: state.isSavingReady,
    selectedFile: state.selectedFile,
    isUploading: state.isUploading,
    isLoadingFiles: state.isLoadingFiles,
    deletingFilename: state.deletingFilename,
    isClearingAll: state.isClearingAll,
    dataFiles: state.dataFiles,
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
    deleteDataFile,
    deleteDataPath,
    downloadDataPathUrl: (path: ProblemDataPath) => new DownloadProblemDataPath(problemSlug, path).downloadUrl(),
    clearAllDataFiles,
    setReady,
  }
}
