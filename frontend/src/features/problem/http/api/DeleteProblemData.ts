import type {
  ProblemDataFilename,
  ProblemDetail,
  ProblemSlug,
} from '@/features/problem/domain/problem'
import { problemSlugValue } from '@/features/problem/domain/problem'
import { fromProblemDetailContract } from '@/features/problem/http/codec'
import { postJson } from '@/shared/api/http-client'

export async function deleteProblemData(problemSlug: ProblemSlug, filename: ProblemDataFilename): Promise<ProblemDetail> {
  return postJson(
    `/api/problems/${problemSlugValue(problemSlug)}/data/${encodeURIComponent(filename)}/delete`,
    fromProblemDetailContract,
    {},
  )
}
