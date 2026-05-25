import { useCallback, useEffect, useReducer } from 'react'

import { clearProblemData } from '@/features/problem/http/api/ClearProblemData'
import { deleteProblemData } from '@/features/problem/http/api/DeleteProblemData'
import { deleteProblemDataPath } from '@/features/problem/http/api/DeleteProblemDataPath'
import { listProblemDataFiles } from '@/features/problem/http/api/ListProblemDataFiles'
import { listProblemDataTree } from '@/features/problem/http/api/ListProblemDataTree'
import { problemDataPathDownloadUrl } from '@/features/problem/http/api/DownloadProblemDataPath'
import { setProblemDataReady } from '@/features/problem/http/api/SetProblemDataReady'
import { uploadProblemDataArchive } from '@/features/problem/http/api/UploadProblemDataArchive'
import { uploadProblemDataFile } from '@/features/problem/http/api/UploadProblemDataFile'
import { parseProblemDataFilename, problemDataPathValue, problemDataFilenameValue } from '@/features/problem/lib/problem-parsers'
import type { ProblemDataFilename } from '@/features/problem/model/ProblemDataFilename'
import type { ProblemDataPath } from '@/features/problem/model/ProblemDataPath'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import {
  initialProblemDataPageState,
  reduceProblemDataPageState,
} from '@/features/problem/state/problem-data-page-state'
import { useProblemDetailQuery } from '@/features/problem/hooks/use-problem-detail-query'
import { HttpClientError } from '@/shared/api/http-client'
import { useI18n } from '@/shared/i18n/use-i18n'

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
      const [files, tree] = await Promise.all([listProblemDataFiles(problemSlug), listProblemDataTree(problemSlug)])
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
      const updatedProblem = isZipArchive
        ? await uploadProblemDataArchive(problemSlug, state.selectedFile)
        : await uploadProblemDataFile(problemSlug, state.selectedFile, filenameResult.value)

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
        const updatedProblem = await deleteProblemData(problemSlug, filename)
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
        const updatedProblem = await deleteProblemDataPath(problemSlug, path)
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
      const updatedProblem = await clearProblemData(problemSlug)
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
      const updatedProblem = await setProblemDataReady(problemSlug, ready)
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
    downloadDataPathUrl: (path: ProblemDataPath) => problemDataPathDownloadUrl(problemSlug, path),
    clearAllDataFiles,
    setReady,
  }
}
