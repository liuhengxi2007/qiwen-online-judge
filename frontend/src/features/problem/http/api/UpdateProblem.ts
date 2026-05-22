import type {
  ProblemDetail,
  ProblemSlug,
  UpdateProblemRequest,
} from '@/features/problem/domain/problem'
import { problemSlugValue } from '@/features/problem/domain/problem'
import {
  fromProblemDetailContract,
  toUpdateProblemRequestContract,
} from '@/features/problem/http/codec'
import { postJson } from '@/shared/api/http-client'

export async function updateProblem(problemSlug: ProblemSlug, request: UpdateProblemRequest): Promise<ProblemDetail> {
  return postJson(
    `/api/problems/${problemSlugValue(problemSlug)}`,
    fromProblemDetailContract,
    toUpdateProblemRequestContract(request),
  )
}
