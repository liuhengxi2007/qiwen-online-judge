import type { ProblemSlug } from '@/features/problem/domain/problem'
import type {
  ProblemSetDetail,
  ProblemSetSlug,
} from '@/features/problemset/domain/problemset'
import { problemSetSlugValue } from '@/features/problemset/domain/problemset'
import { fromProblemSetDetailContract } from '@/features/problemset/http/codec'
import { postJson } from '@/shared/api/http-client'

export function removeProblemFromProblemSet(
  problemSetSlug: ProblemSetSlug,
  problemSlug: ProblemSlug,
): Promise<ProblemSetDetail> {
  return removeProblemFromProblemSetInternal(problemSetSlug, problemSlug)
}

async function removeProblemFromProblemSetInternal(
  problemSetSlug: ProblemSetSlug,
  problemSlug: ProblemSlug,
): Promise<ProblemSetDetail> {
  return postJson(
    `/api/problem-sets/${problemSetSlugValue(problemSetSlug)}/problems/${problemSlug}/remove`,
    fromProblemSetDetailContract,
    {},
  )
}
