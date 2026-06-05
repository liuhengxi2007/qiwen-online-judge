import { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { CreateContestSubmission } from '@/apis/submission/CreateContestSubmission'
import { CreateSubmission } from '@/apis/submission/CreateSubmission'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { CreateSubmissionRequest } from '@/objects/submission/request/CreateSubmissionRequest'
import { submissionIdValue } from '@/objects/submission/SubmissionId'
import { sendAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'

export function useCreateSubmissionAction(createFailedMessage: string, contestSlug?: ContestSlug) {
  const navigate = useNavigate()
  const [statusMessage, setStatusMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const submit = useCallback(
    async (request: CreateSubmissionRequest) => {
      setIsSubmitting(true)
      setErrorMessage('')
      setStatusMessage('')

      try {
        const submission = await sendAPI(contestSlug ? new CreateContestSubmission(contestSlug, request) : new CreateSubmission(request))
        navigate(`/submissions/${submissionIdValue(submission.id)}`)
      } catch (error) {
        setErrorMessage(error instanceof HttpClientError ? error.message : createFailedMessage)
      } finally {
        setIsSubmitting(false)
      }
    },
    [contestSlug, createFailedMessage, navigate],
  )

  const clearMessages = useCallback(() => {
    setStatusMessage('')
    setErrorMessage('')
  }, [])

  return {
    statusMessage,
    errorMessage,
    isSubmitting,
    setErrorMessage,
    clearMessages,
    submit,
  }
}
