import type { ProblemId } from '@/objects/problem/ProblemId'
import { fromProblemIdContract } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { fromProblemSlugContract, toProblemSlugContract } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import { fromProblemTitleContract } from '@/objects/problem/ProblemTitle'

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
  if (!Number.isInteger(rawPosition) || rawPosition <= 0) {
    return { ok: false, error: 'Problem set problem position must be a positive integer.' }
  }
  return { ok: true, value: rawPosition }
}

export function fromProblemSetProblemSummaryContract(
  problem: ProblemSetProblemSummaryContract,
): ProblemSetProblemSummary {
  const positionResult = parseProblemSetProblemPosition(problem.position)
  if (!positionResult.ok) {
    throw new Error(`Invalid problem set problem position in contract payload: ${positionResult.error}`)
  }

  return {
    id: fromProblemIdContract(problem.id, 'problem set problem id'),
    slug: fromProblemSlugContract(problem.slug, 'problem set problem slug'),
    title: fromProblemTitleContract(problem.title, 'problem set problem title'),
    position: positionResult.value,
  }
}

export function toProblemSetProblemSlugContract(value: ProblemSetProblemSummary['slug']): string {
  return toProblemSlugContract(value)
}
