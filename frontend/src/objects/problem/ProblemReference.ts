import type { ProblemId } from '@/objects/problem/ProblemId'
import { fromProblemIdContract } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { fromProblemSlugContract } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import { fromProblemTitleContract } from '@/objects/problem/ProblemTitle'
import { readRecord, readString } from '@/objects/shared/PageResponse'

export type ProblemReference = {
  id: ProblemId
  slug: ProblemSlug
  title: ProblemTitle
}

export function fromProblemReferenceContract(value: unknown, label: string): ProblemReference {
  const problem = readRecord(value, label)
  return {
    id: fromProblemIdContract(readString(problem.id, `${label} id`), `${label} id`),
    slug: fromProblemSlugContract(readString(problem.slug, `${label} slug`), `${label} slug`),
    title: fromProblemTitleContract(readString(problem.title, `${label} title`), `${label} title`),
  }
}
