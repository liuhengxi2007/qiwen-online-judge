import { Navigate, useParams } from 'react-router-dom'

import { parseContestSlug } from '@/objects/contest/ContestSlug'
import { SubmissionListPageContent } from '@/pages/SubmissionPage/SubmissionListPageContent'

export function ContestSubmissionPage() {
  const { slug } = useParams<{ slug: string }>()
  const slugResult = parseContestSlug(slug ?? '')

  if (!slugResult.ok) {
    return <Navigate replace to="/contests" />
  }

  return <SubmissionListPageContent contestSlug={slugResult.value} titleKey="contest.submissions.heading" />
}
