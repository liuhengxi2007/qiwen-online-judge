import { Navigate, useParams } from 'react-router-dom'

import { parseContestSlug } from '@/objects/contest/ContestSlug'
import { SubmissionListPageContent } from '@/pages/components/submission/SubmissionListPageContent'

/**
 * 比赛提交列表页，解析比赛 slug 并复用提交列表内容组件。
 */
export function ContestSubmissionPage() {
  const { slug } = useParams<{ slug: string }>()
  const slugResult = parseContestSlug(slug ?? '')

  if (!slugResult.ok) {
    return <Navigate replace to="/contests" />
  }

  return <SubmissionListPageContent contestSlug={slugResult.value} titleKey="contest.submissions.heading" />
}
