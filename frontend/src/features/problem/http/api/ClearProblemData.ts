import type { ProblemDetail } from '@/features/problem/http/response/ProblemDetail'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import { problemSlugValue } from '@/features/problem/lib/problem-parsers'
import { fromProblemDetailContract } from '@/features/problem/http/codec/ProblemHttpCodecs'
import { postJson } from '@/shared/api/http-client'

export async function clearProblemData(problemSlug: ProblemSlug): Promise<ProblemDetail> {
  return postJson(
    `/api/problems/${problemSlugValue(problemSlug)}/data/clear`,
    fromProblemDetailContract,
    {},
  )
}
