import type { ProblemId } from '@/objects/problem/ProblemId'
import { fromProblemIdContract } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { fromProblemSlugContract, toProblemSlugContract } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import { fromProblemTitleContract } from '@/objects/problem/ProblemTitle'
import { readPositiveSafeInteger, readRecord, readString } from '@/objects/shared/PageResponse'

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

type ProblemSetProblemSummaryContract = {
  id: string
  slug: string
  title: string
  position: number
}

export type ProblemSetProblemSummary = {
  id: ProblemId
  slug: ProblemSlug
  title: ProblemTitle
  position: number
}

export function parseProblemSetProblemPosition(rawPosition: number): ParseResult<number> {
  if (!Number.isSafeInteger(rawPosition) || rawPosition <= 0) {
    return { ok: false, error: 'Problem set problem position must be a positive integer.' }
  }
  return { ok: true, value: rawPosition }
}

export function fromProblemSetProblemSummaryContract(
  value: unknown,
  label = 'problem set problem',
): ProblemSetProblemSummary {
  const problem = readRecord(value, label) as ProblemSetProblemSummaryContract
  const position = readPositiveSafeInteger(problem.position, `${label} position`)

  return {
    id: fromProblemIdContract(readString(problem.id, `${label} id`), `${label} id`),
    slug: fromProblemSlugContract(readString(problem.slug, `${label} slug`), `${label} slug`),
    title: fromProblemTitleContract(readString(problem.title, `${label} title`), `${label} title`),
    position,
  }
}

export function toProblemSetProblemSlugContract(value: ProblemSetProblemSummary['slug']): string {
  return toProblemSlugContract(value)
}
