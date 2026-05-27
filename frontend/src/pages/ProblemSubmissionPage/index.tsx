import { Navigate, useParams } from 'react-router-dom'

import { parseProblemSlug } from '@/objects/problem/problem-parsers'
import { SubmissionListPageContent } from '@/pages/SubmissionPage/SubmissionListPageContent'

export function ProblemSubmissionPage() {
  const { slug } = useParams<{ slug: string }>()
  const slugResult = parseProblemSlug(slug ?? '')

  if (!slugResult.ok) {
    return <Navigate replace to="/problems" />
  }

  return <SubmissionListPageContent fixedProblemSlugFilter={slugResult.value} />
}
