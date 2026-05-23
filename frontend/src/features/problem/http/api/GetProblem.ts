import type { ProblemDetail } from '@/features/problem/http/response/ProblemDetail'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import { problemSlugValue } from '@/features/problem/lib/problem-parsers'
import { fromProblemDetailContract } from '@/features/problem/http/codec'
import { requestJson } from '@/shared/api/http-client'

export async function getProblem(problemSlug: ProblemSlug): Promise<ProblemDetail> {
  return requestJson(`/api/problems/${problemSlugValue(problemSlug)}`, fromProblemDetailContract)
}
