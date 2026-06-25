import { Navigate, useParams } from 'react-router-dom'

import { parseContestSlug } from '@/objects/contest/ContestSlug'
import { SubmissionListPageContent } from '@/pages/components/SubmissionListPageContent'

/**
 * 比赛提交列表页，解析比赛 slug 并复用提交列表内容组件。
 */
export function ContestSubmissionPage() {
  const { slug } = useParams<{ slug: string }>()
  const slugResult = parseContestSlug(slug ?? '')

  if (!slugResult.ok) {
    return <Navigate replace to="/contests" />
  }

  // 保留共享提交列表内容组件：这里和 SubmissionPage、ProblemSubmissionPage 共用筛选、分页和列表逻辑。
  return <SubmissionListPageContent contestSlug={slugResult.value} titleKey="contest.submissions.heading" />
}
