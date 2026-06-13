import { Navigate, useParams } from 'react-router-dom'

import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import { SubmissionListPageContent } from '@/pages/components/submission/SubmissionListPageContent'

/**
 * 题目提交列表页，解析题目 slug 后用固定题目过滤条件展示提交列表。
 * 非法 slug 回到题目列表，避免提交列表拿到无效领域值。
 */
export function ProblemSubmissionPage() {
  const { slug } = useParams<{ slug: string }>()
  const slugResult = parseProblemSlug(slug ?? '')

  if (!slugResult.ok) {
    return <Navigate replace to="/problems" />
  }

  return <SubmissionListPageContent fixedProblemSlugFilter={slugResult.value} />
}
