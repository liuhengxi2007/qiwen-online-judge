import { useCallback, useEffect, useReducer } from 'react'

import {
  clearProblemData,
  deleteProblemData,
  deleteProblemDataPath,
  listProblemDataFiles,
  listProblemDataTree,
  uploadProblemDataArchive,
  uploadProblemDataFile,
  updateProblem,
} from '@/features/problem/api/problem-client'
import {
  parseProblemDataFilename,
  problemDataPathValue,
  parseProblemSpaceLimitMb,
  parseProblemTimeLimitMs,
  problemDataFilenameValue,
  type ProblemDataFilename,
  type ProblemSlug,
} from '@/features/problem/domain/problem'
import {
  initialProblemDataPageState,
  reduceProblemDataPageState,
} from '@/features/problem/domain/problem-data-page-state'
import { useProblemDetailQuery } from '@/features/problem/hooks/use-problem-detail-query'
import { HttpClientError } from '@/shared/api/http-client'
import { useI18n } from '@/shared/i18n/i18n'

type UploadResult = { ok: true } | { ok: false; message: string }
type DeleteResult = { ok: true } | { ok: false; message: string }

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

  useEffect(() => {
    if (!problem) {
      return
    }

    dispatch({
      type: 'problem_hydrated',
      timeLimitMs: problem.timeLimitMs,
      spaceLimitMb: problem.spaceLimitMb,
    })
  }, [problem])

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
    async (path: import('@/features/problem/domain/problem').ProblemDataPath): Promise<DeleteResult> => {
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

  const saveLimits = useCallback(async (): Promise<DeleteResult> => {
    const currentProblem = problem
    if (!currentProblem) {
      const message = t('problem.data.loadFailed')
      dispatch({ type: 'limits_save_failed', message })
      return { ok: false, message }
    }

    dispatch({ type: 'limits_save_started' })

    const timeLimitResult = parseProblemTimeLimitMs(state.timeLimitMs)
    if (!timeLimitResult.ok) {
      dispatch({ type: 'limits_save_failed', message: timeLimitResult.error })
      return { ok: false, message: timeLimitResult.error }
    }

    const spaceLimitResult = parseProblemSpaceLimitMb(state.spaceLimitMb)
    if (!spaceLimitResult.ok) {
      dispatch({ type: 'limits_save_failed', message: spaceLimitResult.error })
      return { ok: false, message: spaceLimitResult.error }
    }

    try {
      const updatedProblem = await updateProblem(problemSlug, {
        title: currentProblem.title,
        statement: currentProblem.statement,
        timeLimitMs: timeLimitResult.value,
        spaceLimitMb: spaceLimitResult.value,
        accessPolicy: currentProblem.accessPolicy,
        othersSubmissionAccess: currentProblem.othersSubmissionAccess,
      })
      replaceProblem(updatedProblem)
      dispatch({
        type: 'limits_save_succeeded',
        timeLimitMs: updatedProblem.timeLimitMs,
        spaceLimitMb: updatedProblem.spaceLimitMb,
        message: t('problem.message.updateSuccess'),
      })
      return { ok: true }
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('problem.message.updateFailed')
      dispatch({ type: 'limits_save_failed', message })
      return { ok: false, message }
    }
  }, [problem, problemSlug, replaceProblem, state.spaceLimitMb, state.timeLimitMs, t])

  return {
    problem,
    isProblemLoading: detailQuery.isLoading,
    problemErrorMessage: detailQuery.errorMessage,
    timeLimitMs: state.timeLimitMs,
    spaceLimitMb: state.spaceLimitMb,
    isSavingLimits: state.isSavingLimits,
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
    setTimeLimitMs: (value: number) => dispatch({ type: 'time_limit_set', value }),
    setSpaceLimitMb: (value: number) => dispatch({ type: 'space_limit_set', value }),
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
    clearAllDataFiles,
    saveLimits,
  }
}
