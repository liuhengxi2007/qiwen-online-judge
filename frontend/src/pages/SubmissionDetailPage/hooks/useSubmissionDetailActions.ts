import { useCallback, useState } from 'react'

import { DeleteSubmission } from '@/apis/submission/DeleteSubmission'
import { RejudgeSubmission } from '@/apis/submission/RejudgeSubmission'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'

/**
 * 提交详情操作 hook 参数，包含目标提交、替换详情回调和本地化失败文案。
 */
type UseSubmissionDetailActionsOptions = {
  submissionId: SubmissionId
  replaceSubmission: (submission: SubmissionDetail) => void
  rejudgeFailedMessage: string
  deleteFailedMessage: string
}

/**
 * 提交详情操作 hook，封装重测和删除提交的 API 副作用。
 * 重测成功会用返回详情替换页面状态，删除成功只标记 deleted 由页面负责跳转。
 */
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
      const submission = await sendAPI(new RejudgeSubmission(submissionId))
      replaceSubmission(submission)
    } catch (error) {
      setActionErrorMessage(isHttpClientError(error) ? error.message : rejudgeFailedMessage)
    } finally {
      setIsRejudging(false)
    }
  }, [replaceSubmission, rejudgeFailedMessage, submissionId])

  const deleteCurrentSubmission = useCallback(async () => {
    setActionErrorMessage('')
    setIsDeleting(true)
    try {
      await sendAPI(new DeleteSubmission(submissionId))
      setDeleted(true)
    } catch (error) {
      setActionErrorMessage(isHttpClientError(error) ? error.message : deleteFailedMessage)
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
