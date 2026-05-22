import type {
  ProblemDetail,
  ProblemSlug,
} from '@/features/problem/domain/problem'
import {
  fromProblemDetailContract,
  problemSlugValue,
} from '@/features/problem/domain/problem'
import { postJson } from '@/shared/api/http-client'

export async function clearProblemData(problemSlug: ProblemSlug): Promise<ProblemDetail> {
  return postJson(
    `/api/problems/${problemSlugValue(problemSlug)}/data/clear`,
    fromProblemDetailContract,
    {},
  )
}
