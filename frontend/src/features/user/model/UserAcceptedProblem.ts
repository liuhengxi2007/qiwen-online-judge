import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import type { ProblemTitle } from '@/features/problem/model/ProblemTitle'

export type UserAcceptedProblem = {
  slug: ProblemSlug
  title: ProblemTitle
  acceptedAt: string
}
