import type { DeleteProblemDataPathRequest } from '@/features/problem/http/request/DeleteProblemDataPathRequest'
import type { ProblemDataPath } from '@/features/problem/model/ProblemDataPath'
import type { ProblemDetail } from '@/features/problem/http/response/ProblemDetail'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import { problemDataPathValue, problemSlugValue } from '@/features/problem/lib/problem-parsers'
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
