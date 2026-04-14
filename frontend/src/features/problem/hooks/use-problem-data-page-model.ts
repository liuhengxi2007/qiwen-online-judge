import { useCallback, useEffect, useState } from 'react'

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
import { useProblemDetailQuery } from '@/features/problem/hooks/use-problem-detail-query'
import { HttpClientError } from '@/shared/api/http-client'
import { useI18n } from '@/shared/i18n/i18n'

type UploadResult = { ok: true } | { ok: false; message: string }
type DeleteResult = { ok: true } | { ok: false; message: string }

export function useProblemDataPageModel(problemSlug: ProblemSlug) {
  const { t } = useI18n()
  const detailQuery = useProblemDetailQuery(problemSlug)
  const replaceProblem = detailQuery.replaceProblem
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [isUploading, setIsUploading] = useState(false)
  const [isLoadingFiles, setIsLoadingFiles] = useState(true)
  const [deletingFilename, setDeletingFilename] = useState<ProblemDataFilename | null>(null)
  const [isClearingAll, setIsClearingAll] = useState(false)
  const [dataFiles, setDataFiles] = useState<ProblemDataFilename[]>([])
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const loadFiles = useCallback(async () => {
    setIsLoadingFiles(true)
    try {
      const files = await listProblemDataFiles(problemSlug)
      setDataFiles(files.items)
      return { ok: true as const }
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('problem.data.loadFailed')
      setErrorMessage(message)
      return { ok: false as const, message }
    } finally {
      setIsLoadingFiles(false)
    }
  }, [problemSlug])

  useEffect(() => {
    void loadFiles()
  }, [loadFiles])

  const uploadSelectedFile = useCallback(async (): Promise<UploadResult> => {
    if (!selectedFile) {
      const message = 'Please choose a file to upload.'
      setErrorMessage(message)
      setSuccessMessage('')
      return { ok: false, message }
    }

    const filenameResult = parseProblemDataFilename(selectedFile.name)
    if (!filenameResult.ok) {
      setErrorMessage(filenameResult.error)
      setSuccessMessage('')
      return { ok: false, message: filenameResult.error }
    }

    setIsUploading(true)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      const buffer = await selectedFile.arrayBuffer()
      const bytes = new Uint8Array(buffer)
      let binary = ''
      bytes.forEach((byte) => {
        binary += String.fromCharCode(byte)
      })

      const updatedProblem = await updateProblemData(problemSlug, {
        filename: filenameResult.value,
        contentBase64: btoa(binary),
      })

      replaceProblem(updatedProblem)
      setSuccessMessage(
        `Uploaded ${updatedProblem.data.value ?? problemDataFilenameValue(filenameResult.value)} successfully.`,
      )
      setSelectedFile(null)
      await loadFiles()
      return { ok: true }
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : 'Unable to upload problem data.'
      setErrorMessage(message)
      return { ok: false, message }
    } finally {
      setIsUploading(false)
    }
  }, [loadFiles, problemSlug, replaceProblem, selectedFile])

  const deleteDataFile = useCallback(
    async (filename: ProblemDataFilename): Promise<DeleteResult> => {
      setDeletingFilename(filename)
      setErrorMessage('')
      setSuccessMessage('')

      try {
        const updatedProblem = await deleteProblemData(problemSlug, filename)
        replaceProblem(updatedProblem)
        setSuccessMessage(`Deleted ${problemDataFilenameValue(filename)} successfully.`)
        await loadFiles()
        return { ok: true }
      } catch (error) {
        const message = error instanceof HttpClientError ? error.message : 'Unable to delete problem data.'
        setErrorMessage(message)
        return { ok: false, message }
      } finally {
        setDeletingFilename(null)
      }
    },
    [loadFiles, problemSlug, replaceProblem],
  )

  const clearAllDataFiles = useCallback(async (): Promise<DeleteResult> => {
    setIsClearingAll(true)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      const updatedProblem = await clearProblemData(problemSlug)
      replaceProblem(updatedProblem)
      setSuccessMessage('Cleared all data files successfully.')
      await loadFiles()
      return { ok: true }
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : 'Unable to clear problem data.'
      setErrorMessage(message)
      return { ok: false, message }
    } finally {
      setIsClearingAll(false)
    }
  }, [loadFiles, problemSlug, replaceProblem])

  return {
    problem: detailQuery.problem,
    isProblemLoading: detailQuery.isLoading,
    problemErrorMessage: detailQuery.errorMessage,
    selectedFile,
    isUploading,
    isLoadingFiles,
    deletingFilename,
    isClearingAll,
    dataFiles,
    errorMessage,
    successMessage,
    setSelectedFile,
    setErrorMessage,
    setSuccessMessage,
    uploadSelectedFile,
    deleteDataFile,
    clearAllDataFiles,
  }
}
