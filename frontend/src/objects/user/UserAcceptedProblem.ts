import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { fromProblemSlugContract } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import { fromProblemTitleContract } from '@/objects/problem/ProblemTitle'

export type UserAcceptedProblem = {
  slug: ProblemSlug
  title: ProblemTitle
  acceptedAt: string
}

type UserAcceptedProblemContract = {
  slug: string
  title: string
  acceptedAt: string
}

export function fromUserAcceptedProblemContract(
  problem: UserAcceptedProblemContract,
  index: number,
): UserAcceptedProblem {
  return {
    slug: fromProblemSlugContract(problem.slug, `user profile accepted problem slug ${index}`),
    title: fromProblemTitleContract(problem.title, `user profile accepted problem title ${index}`),
    acceptedAt: problem.acceptedAt,
  }
}
