import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }
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