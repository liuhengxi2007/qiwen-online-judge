import type { ProblemDetail } from '@/features/problem/model/response/ProblemDetail'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import type { UpdateProblemRequest } from '@/features/problem/model/request/UpdateProblemRequest'
import { problemSlugValue } from '@/features/problem/lib/problem-parsers'
import {
  fromProblemDetailContract,
  toUpdateProblemRequestContract,
} from '@/features/problem/http/codec/ProblemHttpCodecs'
import { postJson } from '@/shared/api/http-client'

export async function updateProblem(problemSlug: ProblemSlug, request: UpdateProblemRequest): Promise<ProblemDetail> {
  return postJson(
    `/api/problems/${problemSlugValue(problemSlug)}`,
    fromProblemDetailContract,
    toUpdateProblemRequestContract(request),
  )
}
