import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/problem-parsers'
import { fromProblemDetailContract } from '@/apis/problem/codecs/ProblemHttpCodecs'
import { requestJson } from '@/system/api/http-client'

export async function getProblem(problemSlug: ProblemSlug): Promise<ProblemDetail> {
  return requestJson(`/api/problems/${problemSlugValue(problemSlug)}`, fromProblemDetailContract)
}
