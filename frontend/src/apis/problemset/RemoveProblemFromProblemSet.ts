import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { problemSetSlugValue } from '@/objects/problemset/ProblemSetSlug'
import { fromProblemSetDetailContract } from '@/apis/problemset/codecs/ProblemSetHttpCodecs'
import { postJson } from '@/system/api/http-client'

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
