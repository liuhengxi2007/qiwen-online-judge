import type {
  ProblemDetail,
  ProblemSlug,
} from '@/features/problem/domain/problem'
import { problemSlugValue } from '@/features/problem/domain/problem'
import { fromProblemDetailContract } from '@/features/problem/http/codec'
import { postJson } from '@/shared/api/http-client'

export async function setProblemDataReady(problemSlug: ProblemSlug, ready: boolean): Promise<ProblemDetail> {
  return postJson(
    `/api/problems/${problemSlugValue(problemSlug)}/data/ready`,
    fromProblemDetailContract,
    { ready },
  )
}
