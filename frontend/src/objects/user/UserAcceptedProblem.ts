import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'

export type UserAcceptedProblem = {
  slug: ProblemSlug
  title: ProblemTitle
  acceptedAt: string
}
