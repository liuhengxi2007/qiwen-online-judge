import { useCallback, useState } from 'react'

import { deleteSubmission } from '@/apis/submission/DeleteSubmission'
import { rejudgeSubmission } from '@/apis/submission/RejudgeSubmission'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { HttpClientError } from '@/system/api/http-client'

type UseSubmissionDetailActionsOptions = {
  submissionId: SubmissionId
  replaceSubmission: (submission: SubmissionDetail) => void
  rejudgeFailedMessage: string
  deleteFailedMessage: string
}

export function useSubmissionDetailActions({
  submissionId,
  replaceSubmission,
  rejudgeFailedMessage,
  deleteFailedMessage,
}: UseSubmissionDetailActionsOptions) {
  const [actionErrorMessage, setActionErrorMessage] = useState('')
  const [isDeleting, setIsDeleting] = useState(false)
  const [isRejudging, setIsRejudging] = useState(false)
  const [deleted, setDeleted] = useState(false)

  const rejudge = useCallback(async () => {
    setActionErrorMessage('')
    setIsRejudging(true)
    try {
      const submission = await rejudgeSubmission(submissionId)
      replaceSubmission(submission)
    } catch (error) {
      setActionErrorMessage(error instanceof HttpClientError ? error.message : rejudgeFailedMessage)
    } finally {
      setIsRejudging(false)
    }
  }, [replaceSubmission, rejudgeFailedMessage, submissionId])

  const deleteCurrentSubmission = useCallback(async () => {
    setActionErrorMessage('')
    setIsDeleting(true)
    try {
      await deleteSubmission(submissionId)
      setDeleted(true)
    } catch (error) {
      setActionErrorMessage(error instanceof HttpClientError ? error.message : deleteFailedMessage)
    } finally {
      setIsDeleting(false)
    }
  }, [deleteFailedMessage, submissionId])

  return {
    actionErrorMessage,
    isDeleting,
    isRejudging,
    deleted,
    rejudge,
    deleteCurrentSubmission,
  }
}
