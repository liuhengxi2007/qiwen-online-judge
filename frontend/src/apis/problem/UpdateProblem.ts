import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { UpdateProblemRequest } from '@/objects/problem/request/UpdateProblemRequest'
import { problemSlugValue } from '@/objects/problem/problem-parsers'
import {
  fromProblemDetailContract,
  toUpdateProblemRequestContract,
} from '@/apis/problem/codecs/ProblemHttpCodecs'
import { postJson } from '@/system/api/http-client'

export async function updateProblem(problemSlug: ProblemSlug, request: UpdateProblemRequest): Promise<ProblemDetail> {
  return postJson(
    `/api/problems/${problemSlugValue(problemSlug)}`,
    fromProblemDetailContract,
    toUpdateProblemRequestContract(request),
  )
}
