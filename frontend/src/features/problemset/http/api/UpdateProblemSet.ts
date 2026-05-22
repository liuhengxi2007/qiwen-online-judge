import type {
  ProblemSetDetail,
  ProblemSetSlug,
  UpdateProblemSetRequest,
} from '@/features/problemset/domain/problemset'
import { problemSetSlugValue } from '@/features/problemset/domain/problemset'
import {
  fromProblemSetDetailContract,
  toUpdateProblemSetRequestContract,
} from '@/features/problemset/http/codec'
import { postJson } from '@/shared/api/http-client'

export function updateProblemSet(
  problemSetSlug: ProblemSetSlug,
  request: UpdateProblemSetRequest,
): Promise<ProblemSetDetail> {
  return updateProblemSetInternal(problemSetSlug, request)
}

async function updateProblemSetInternal(
  problemSetSlug: ProblemSetSlug,
  request: UpdateProblemSetRequest,
): Promise<ProblemSetDetail> {
  return postJson(
    `/api/problem-sets/${problemSetSlugValue(problemSetSlug)}`,
    fromProblemSetDetailContract,
    toUpdateProblemSetRequestContract(request),
  )
}
