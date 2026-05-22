import type {
  DeleteProblemDataPathRequest,
  ProblemDataPath,
  ProblemDetail,
  ProblemSlug,
} from '@/features/problem/domain/problem'
import {
  problemDataPathValue,
  problemSlugValue,
} from '@/features/problem/domain/problem'
import { fromProblemDetailContract } from '@/features/problem/http/codec'
import { postJson } from '@/shared/api/http-client'

export async function deleteProblemDataPath(problemSlug: ProblemSlug, path: ProblemDataPath): Promise<ProblemDetail> {
  const request: DeleteProblemDataPathRequest = { path }
  return postJson(
    `/api/problems/${problemSlugValue(problemSlug)}/data/file/delete`,
    fromProblemDetailContract,
    { path: problemDataPathValue(request.path) },
  )
}
