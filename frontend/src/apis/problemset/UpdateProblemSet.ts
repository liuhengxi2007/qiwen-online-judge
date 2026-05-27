import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import type { UpdateProblemSetRequest } from '@/objects/problemset/request/UpdateProblemSetRequest'
import { problemSetSlugValue } from '@/objects/problemset/problemset-parsers'
import {
  fromProblemSetDetailContract,
  toUpdateProblemSetRequestContract,
} from '@/apis/problemset/codecs/ProblemSetHttpCodecs'
import { postJson } from '@/system/api/http-client'

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
