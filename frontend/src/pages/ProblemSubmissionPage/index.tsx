import { Navigate, useParams } from 'react-router-dom'

import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import { SubmissionListPageContent } from '@/pages/components/submission/SubmissionListPageContent'

export function ProblemSubmissionPage() {
  const { slug } = useParams<{ slug: string }>()
  const slugResult = parseProblemSlug(slug ?? '')

  if (!slugResult.ok) {
    return <Navigate replace to="/problems" />
  }

  return <SubmissionListPageContent fixedProblemSlugFilter={slugResult.value} />
}
