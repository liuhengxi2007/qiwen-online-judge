import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import type { ProblemSetDetail } from '@/features/problemset/model/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/features/problemset/model/ProblemSetSlug'
import { problemSetSlugValue } from '@/features/problemset/lib/problemset-parsers'
import { fromProblemSetDetailContract } from '@/features/problemset/http/codec/ProblemSetHttpCodecs'
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
