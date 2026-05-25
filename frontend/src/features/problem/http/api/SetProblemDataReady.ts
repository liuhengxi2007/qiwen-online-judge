import type { ProblemDetail } from '@/features/problem/model/response/ProblemDetail'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import { problemSlugValue } from '@/features/problem/lib/problem-parsers'
import { fromProblemDetailContract } from '@/features/problem/http/codec/ProblemHttpCodecs'
import { postJson } from '@/shared/api/http-client'

export async function setProblemDataReady(problemSlug: ProblemSlug, ready: boolean): Promise<ProblemDetail> {
  return postJson(
    `/api/problems/${problemSlugValue(problemSlug)}/data/ready`,
    fromProblemDetailContract,
    { ready },
  )
}
