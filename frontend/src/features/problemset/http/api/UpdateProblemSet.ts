import type { ProblemSetDetail } from '@/features/problemset/model/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/features/problemset/model/ProblemSetSlug'
import type { UpdateProblemSetRequest } from '@/features/problemset/model/request/UpdateProblemSetRequest'
import { problemSetSlugValue } from '@/features/problemset/lib/problemset-parsers'
import {
  fromProblemSetDetailContract,
  toUpdateProblemSetRequestContract,
} from '@/features/problemset/http/codec/ProblemSetHttpCodecs'
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
