import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { fromProblemSlugContract } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import { fromProblemTitleContract } from '@/objects/problem/ProblemTitle'
import { readRecord, readString } from '@/objects/shared/PageResponse'

export type ProblemSuggestion = {
  slug: ProblemSlug
  title: ProblemTitle
}

export function fromProblemSuggestionContract(value: unknown, label: string): ProblemSuggestion {
  const suggestion = readRecord(value, label)
  return {
    slug: fromProblemSlugContract(readString(suggestion.slug, `${label} slug`), `${label} slug`),
    title: fromProblemTitleContract(readString(suggestion.title, `${label} title`), `${label} title`),
  }
}
