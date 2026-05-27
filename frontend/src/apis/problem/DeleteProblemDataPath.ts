import type { DeleteProblemDataPathRequest } from '@/objects/problem/request/DeleteProblemDataPathRequest'
import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemDataPathValue, problemSlugValue } from '@/objects/problem/problem-parsers'
import { fromProblemDetailContract } from '@/apis/problem/codecs/ProblemHttpCodecs'
import { postJson } from '@/system/api/http-client'

export async function deleteProblemDataPath(problemSlug: ProblemSlug, path: ProblemDataPath): Promise<ProblemDetail> {
  const request: DeleteProblemDataPathRequest = { path }
  return postJson(
    `/api/problems/${problemSlugValue(problemSlug)}/data/file/delete`,
    fromProblemDetailContract,
    { path: problemDataPathValue(request.path) },
  )
}
