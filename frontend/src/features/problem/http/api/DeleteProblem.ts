import type { SuccessResponse } from '@/shared/http/response/SuccessResponse'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import { problemSlugValue } from '@/features/problem/lib/problem-parsers'
import {
  decodeSuccessResponse,
  postJson,
} from '@/shared/api/http-client'

export function deleteProblem(problemSlug: ProblemSlug): Promise<SuccessResponse> {
  return postJson(`/api/problems/${problemSlugValue(problemSlug)}/delete`, decodeSuccessResponse, {})
}
