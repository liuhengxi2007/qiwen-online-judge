import type { ProblemSetDescription } from '@/features/problemset/model/ProblemSetDescription'
import type { ProblemSetId } from '@/features/problemset/model/ProblemSetId'
import type { ProblemSetProblemSummary } from '@/features/problemset/model/ProblemSetProblemSummary'
import type { ProblemSetSlug } from '@/features/problemset/model/ProblemSetSlug'
import type { ProblemSetTitle } from '@/features/problemset/model/ProblemSetTitle'
import {
  fromProblemIdContract,
  fromProblemSlugContract,
  fromProblemTitleContract,
  toProblemSlugContract,
} from '@/features/problem/http/codec/ProblemModelHttpCodecs'
import {
  parseProblemSetDescription,
  parseProblemSetId,
  parseProblemSetProblemPosition,
  parseProblemSetSlug,
  parseProblemSetTitle,
  problemSetDescriptionValue,
  problemSetSlugValue,
  problemSetTitleValue,
  requireParsed,
} from '@/features/problemset/lib/problemset-parsers'

export type ProblemSetIdContract = string
export type ProblemSetSlugContract = string
export type ProblemSetTitleContract = string
export type ProblemSetDescriptionContract = string

export type ProblemSetProblemSummaryContract = {
  id: string
  slug: string
  title: string
  position: number
}

export function fromProblemSetIdContract(value: ProblemSetIdContract, label: string): ProblemSetId {
  return requireParsed(parseProblemSetId(value), label)
}

export function fromProblemSetSlugContract(value: ProblemSetSlugContract, label: string): ProblemSetSlug {
  return requireParsed(parseProblemSetSlug(value), label)
}

export function toProblemSetSlugContract(value: ProblemSetSlug): ProblemSetSlugContract {
  return problemSetSlugValue(value)
}

export function fromProblemSetTitleContract(value: ProblemSetTitleContract, label: string): ProblemSetTitle {
  return requireParsed(parseProblemSetTitle(value), label)
}

export function toProblemSetTitleContract(value: ProblemSetTitle): ProblemSetTitleContract {
  return problemSetTitleValue(value)
}

export function fromProblemSetDescriptionContract(
  value: ProblemSetDescriptionContract,
  label: string,
): ProblemSetDescription {
  return requireParsed(parseProblemSetDescription(value), label)
}

export function toProblemSetDescriptionContract(
  value: ProblemSetDescription,
): ProblemSetDescriptionContract {
  return problemSetDescriptionValue(value)
}

export function fromProblemSetProblemSummaryContract(
  problem: ProblemSetProblemSummaryContract,
): ProblemSetProblemSummary {
  return {
    id: fromProblemIdContract(problem.id, 'problem set problem id'),
    slug: fromProblemSlugContract(problem.slug, 'problem set problem slug'),
    title: fromProblemTitleContract(problem.title, 'problem set problem title'),
    position: requireParsed(parseProblemSetProblemPosition(problem.position), 'problem set problem position'),
  }
}

export function toProblemSetProblemSlugContract(value: ProblemSetProblemSummary['slug']): string {
  return toProblemSlugContract(value)
}
