import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { problemSetSlugValue } from '@/objects/problemset/problemset-parsers'
import {
  decodeSuccessResponse,
  postJson,
} from '@/system/api/http-client'

export function deleteProblemSet(problemSetSlug: ProblemSetSlug): Promise<SuccessResponse> {
  return postJson(`/api/problem-sets/${problemSetSlugValue(problemSetSlug)}/delete`, decodeSuccessResponse, {})
}
