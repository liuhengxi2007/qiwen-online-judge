import { useState } from 'react'

import { SubmitBlogToProblem } from '@/apis/blog/SubmitBlogToProblem'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import { sendAPI } from '@/system/api/api-message'
import { useI18n } from '@/system/i18n/use-i18n'

type UseBlogProblemSubmissionActionsArgs = {
  blog: BlogDetail | null
}

export function useBlogProblemSubmissionActions({ blog }: UseBlogProblemSubmissionActionsArgs) {
  const { t } = useI18n()
  const [submitProblemSlug, setSubmitProblemSlug] = useState('')
  const [isSubmittingToProblem, setIsSubmittingToProblem] = useState(false)
  const [submitProblemMessage, setSubmitProblemMessage] = useState('')

  async function submitToProblem() {
    if (!blog) {
      return
    }

    const parsedProblemSlug = parseProblemSlug(submitProblemSlug)
    if (!parsedProblemSlug.ok) {
      setSubmitProblemMessage(parsedProblemSlug.error)
      return
    }

    setIsSubmittingToProblem(true)
    setSubmitProblemMessage('')
    try {
      await sendAPI(new SubmitBlogToProblem(parsedProblemSlug.value, blog.id))
      setSubmitProblemSlug('')
      setSubmitProblemMessage(t('blog.problem.submitCreated'))
    } catch {
      setSubmitProblemMessage(t('blog.problem.submitFailed'))
    } finally {
      setIsSubmittingToProblem(false)
    }
  }

  return {
    submitProblemSlug,
    setSubmitProblemSlug,
    isSubmittingToProblem,
    submitProblemMessage,
    submitToProblem,
  }
}
