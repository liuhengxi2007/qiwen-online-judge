import { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { CreateContestSubmission, CreateContestSubmissionMultipart } from '@/apis/submission/CreateContestSubmission'
import { CreateSubmission, CreateSubmissionMultipart } from '@/apis/submission/CreateSubmission'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { submissionIdValue } from '@/objects/submission/SubmissionId'
import type { SubmissionCreatePayload } from '@/pages/ProblemSubmitPage/functions/SubmitPrograms'
import { sendAPI, sendMultipartAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'

export function useCreateSubmissionAction(createFailedMessage: string, contestSlug?: ContestSlug) {
  const navigate = useNavigate()
  const [statusMessage, setStatusMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const submit = useCallback(
    async (payload: SubmissionCreatePayload) => {
      setIsSubmitting(true)
      setErrorMessage('')
      setStatusMessage('')

      try {
        const submission = await (() => {
          if (payload.kind === 'multipart') {
            const api = contestSlug
              ? new CreateContestSubmissionMultipart(contestSlug, payload.request)
              : new CreateSubmissionMultipart(payload.request)
            return sendMultipartAPI(api, api.formData())
          }

          return sendAPI(contestSlug ? new CreateContestSubmission(contestSlug, payload.request) : new CreateSubmission(payload.request))
        })()
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
