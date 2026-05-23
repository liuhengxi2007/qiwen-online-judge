import type { ProblemDataFilename } from '@/features/problem/model/ProblemDataFilename'
import type { ProblemDetail } from '@/features/problem/http/response/ProblemDetail'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import { problemSlugValue } from '@/features/problem/lib/problem-parsers'
import { fromProblemDetailContract } from '@/features/problem/http/codec'
import { postJson } from '@/shared/api/http-client'

export async function deleteProblemData(problemSlug: ProblemSlug, filename: ProblemDataFilename): Promise<ProblemDetail> {
  return postJson(
    `/api/problems/${problemSlugValue(problemSlug)}/data/${encodeURIComponent(filename)}/delete`,
    fromProblemDetailContract,
    {},
  )
}
