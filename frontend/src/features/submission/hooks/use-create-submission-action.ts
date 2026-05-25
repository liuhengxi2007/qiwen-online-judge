import { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { createSubmission } from '@/features/submission/http/api/CreateSubmission'
import type { CreateSubmissionRequest } from '@/features/submission/http/request/CreateSubmissionRequest'
import { submissionIdValue } from '@/features/submission/lib/submission-parsers'
import { HttpClientError } from '@/shared/api/http-client'

export function useCreateSubmissionAction(createFailedMessage: string) {
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
        const submission = await createSubmission(request)
        navigate(`/submissions/${submissionIdValue(submission.id)}`)
      } catch (error) {
        setErrorMessage(error instanceof HttpClientError ? error.message : createFailedMessage)
      } finally {
        setIsSubmitting(false)
      }
    },
    [createFailedMessage, navigate],
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
