import type { ProblemSetDetail } from '@/features/problemset/http/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/features/problemset/model/ProblemSetSlug'
import type { UpdateProblemSetRequest } from '@/features/problemset/http/request/UpdateProblemSetRequest'
import { problemSetSlugValue } from '@/features/problemset/lib/problemset-parsers'
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
