import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { fromProblemSlugContract } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import { fromProblemTitleContract } from '@/objects/problem/ProblemTitle'
import { readRecord, readString } from '@/objects/shared/PageResponse'

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
  value: unknown,
  label: string,
): UserAcceptedProblem {
  const problem = readRecord(value, label) as UserAcceptedProblemContract
  return {
    slug: fromProblemSlugContract(readString(problem.slug, `${label} slug`), `${label} slug`),
    title: fromProblemTitleContract(readString(problem.title, `${label} title`), `${label} title`),
    acceptedAt: readString(problem.acceptedAt, `${label} accepted at`),
  }
}
