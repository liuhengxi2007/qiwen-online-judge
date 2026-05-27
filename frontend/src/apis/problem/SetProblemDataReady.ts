import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/problem-parsers'
import { fromProblemDetailContract } from '@/apis/problem/codecs/ProblemHttpCodecs'
import { postJson } from '@/system/api/http-client'

export async function setProblemDataReady(problemSlug: ProblemSlug, ready: boolean): Promise<ProblemDetail> {
  return postJson(
    `/api/problems/${problemSlugValue(problemSlug)}/data/ready`,
    fromProblemDetailContract,
    { ready },
  )
}
