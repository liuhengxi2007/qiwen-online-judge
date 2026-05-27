import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/problem-parsers'
import {
  decodeSuccessResponse,
  postJson,
} from '@/system/api/http-client'

export function deleteProblem(problemSlug: ProblemSlug): Promise<SuccessResponse> {
  return postJson(`/api/problems/${problemSlugValue(problemSlug)}/delete`, decodeSuccessResponse, {})
}
