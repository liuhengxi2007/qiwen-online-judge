import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { fromProblemSlugContract } from '@/objects/problem/ProblemSlug'
import { readRecord, readString } from '@/objects/shared/PageResponse'
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
  value: unknown,
  label: string,
): BlogProblemReference {
  const problem = readRecord(value, label) as BlogProblemReferenceContract
  return {
    slug: fromProblemSlugContract(
      readString(problem.slug, `${label} slug`),
      `${label} slug`,
    ),
    title: fromProblemTitleContract(
      readString(problem.title, `${label} title`),
      `${label} title`,
    ),
  }
}
