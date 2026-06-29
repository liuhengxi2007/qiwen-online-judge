import { useState } from 'react'

import { SubmitBlogToProblem } from '@/apis/blog/SubmitBlogToProblem'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import { sendAPI } from '@/system/api/api-message'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 博客提交到题目的 hook 参数，当前仅需要博客详情以取得博客 id。
 */
type UseBlogProblemSubmissionActionsArgs = {
  blog: BlogDetail | null
}

/**
 * 博客提交到题目 hook，校验题目 slug 并创建题目关联提交。
 * 成功后清空 slug 输入并显示状态消息，不在此处重新拉取博客详情。
 */
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
