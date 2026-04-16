import { useCallback, useEffect, useReducer } from 'react'

import {
  clearProblemData,
  deleteProblemData,
  listProblemDataFiles,
  updateProblemData,
} from '@/features/problem/api/problem-client'
import {
  parseProblemDataFilename,
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
  const replaceProblem = detailQuery.replaceProblem
  const [state, dispatch] = useReducer(reduceProblemDataPageState, initialProblemDataPageState)

  const loadFiles = useCallback(async () => {
    dispatch({ type: 'load_started' })
    try {
      const files = await listProblemDataFiles(problemSlug)
      dispatch({ type: 'load_succeeded', files: files.items })
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
      const buffer = await state.selectedFile.arrayBuffer()
      const bytes = new Uint8Array(buffer)
      const binary = Array.from(bytes).reduce((text, byte) => text + String.fromCharCode(byte), '')

      const updatedProblem = await updateProblemData(problemSlug, {
        filename: filenameResult.value,
        contentBase64: btoa(binary),
      })

      replaceProblem(updatedProblem)
      dispatch({
        type: 'upload_succeeded',
        message: `Uploaded ${updatedProblem.data.value ?? problemDataFilenameValue(filenameResult.value)} successfully.`,
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

  return {
    problem: detailQuery.problem,
    isProblemLoading: detailQuery.isLoading,
    problemErrorMessage: detailQuery.errorMessage,
    selectedFile: state.selectedFile,
    isUploading: state.isUploading,
    isLoadingFiles: state.isLoadingFiles,
    deletingFilename: state.deletingFilename,
    isClearingAll: state.isClearingAll,
    dataFiles: state.dataFiles,
    errorMessage: state.errorMessage,
    successMessage: state.successMessage,
    setSelectedFile: (file: File | null) => dispatch({ type: 'selected_file_set', file }),
    setErrorMessage: (message: string) =>
      dispatch(message ? { type: 'load_failed', message } : { type: 'error_cleared' }),
    setSuccessMessage: (message: string) =>
      dispatch(message ? { type: 'upload_succeeded', message } : { type: 'success_cleared' }),
    uploadSelectedFile,
    deleteDataFile,
    clearAllDataFiles,
  }
}
