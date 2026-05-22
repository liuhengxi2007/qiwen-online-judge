import type { SuccessResponse } from '@/shared/model/SuccessResponse'
import type { ProblemSlug } from '@/features/problem/domain/problem'
import { problemSlugValue } from '@/features/problem/domain/problem'
import {
  decodeSuccessResponse,
  postJson,
} from '@/shared/api/http-client'

export function deleteProblem(problemSlug: ProblemSlug): Promise<SuccessResponse> {
  return postJson(`/api/problems/${problemSlugValue(problemSlug)}/delete`, decodeSuccessResponse, {})
}
