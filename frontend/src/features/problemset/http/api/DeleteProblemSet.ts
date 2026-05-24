import type { SuccessResponse } from '@/shared/http/response/SuccessResponse'
import type { ProblemSetSlug } from '@/features/problemset/model/ProblemSetSlug'
import { problemSetSlugValue } from '@/features/problemset/lib/problemset-parsers'
import {
  decodeSuccessResponse,
  postJson,
} from '@/shared/api/http-client'

export function deleteProblemSet(problemSetSlug: ProblemSetSlug): Promise<SuccessResponse> {
  return postJson(`/api/problem-sets/${problemSetSlugValue(problemSetSlug)}/delete`, decodeSuccessResponse, {})
}
