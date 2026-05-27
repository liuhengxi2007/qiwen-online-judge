import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { fromProblemSlugContract } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import { fromProblemTitleContract } from '@/objects/problem/ProblemTitle'

export type BlogProblemReference = {
  slug: ProblemSlug
  title: ProblemTitle
}

type BlogProblemReferenceContract = {
  slug: string
  title: string
}

export function fromBlogProblemReferenceContract(
  problem: BlogProblemReferenceContract,
  index: number,
): BlogProblemReference {
  return {
    slug: fromProblemSlugContract(problem.slug, `blog related problem slug ${index}`),
    title: fromProblemTitleContract(problem.title, `blog related problem title ${index}`),
  }
}
