import type {
  ProblemDetail,
  ProblemSlug,
} from '@/features/problem/domain/problem'
import {
  fromProblemDetailContract,
  problemSlugValue,
} from '@/features/problem/domain/problem'
import { requestJson } from '@/shared/api/http-client'

export async function getProblem(problemSlug: ProblemSlug): Promise<ProblemDetail> {
  return requestJson(`/api/problems/${problemSlugValue(problemSlug)}`, fromProblemDetailContract)
}
