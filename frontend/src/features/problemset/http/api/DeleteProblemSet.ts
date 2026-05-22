import type { SuccessResponse } from '@contracts/shared'
import type { ProblemSetSlug } from '@/features/problemset/domain/problemset'
import { problemSetSlugValue } from '@/features/problemset/domain/problemset'
import {
  decodeSuccessResponse,
  postJson,
} from '@/shared/api/http-client'

export function deleteProblemSet(problemSetSlug: ProblemSetSlug): Promise<SuccessResponse> {
  return postJson(`/api/problem-sets/${problemSetSlugValue(problemSetSlug)}/delete`, decodeSuccessResponse, {})
}
