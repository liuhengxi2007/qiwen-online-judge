import type {
  ProblemDetail,
  ProblemSlug,
  UpdateProblemRequest,
} from '@/features/problem/domain/problem'
import {
  fromProblemDetailContract,
  problemSlugValue,
  toUpdateProblemRequestContract,
} from '@/features/problem/domain/problem'
import { postJson } from '@/shared/api/http-client'

export async function updateProblem(problemSlug: ProblemSlug, request: UpdateProblemRequest): Promise<ProblemDetail> {
  return postJson(
    `/api/problems/${problemSlugValue(problemSlug)}`,
    fromProblemDetailContract,
    toUpdateProblemRequestContract(request),
  )
}
