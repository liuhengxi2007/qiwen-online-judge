import type { AddProblemToProblemSetRequest } from '@/objects/problemset/request/AddProblemToProblemSetRequest'
import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { problemSetSlugValue } from '@/objects/problemset/problemset-parsers'
import {
  fromProblemSetDetailContract,
  toAddProblemToProblemSetRequestContract,
} from '@/apis/problemset/codecs/ProblemSetHttpCodecs'
import { postJson } from '@/system/api/http-client'

export function addProblemToProblemSet(
  problemSetSlug: ProblemSetSlug,
  request: AddProblemToProblemSetRequest,
): Promise<ProblemSetDetail> {
  return addProblemToProblemSetInternal(problemSetSlug, request)
}

async function addProblemToProblemSetInternal(
  problemSetSlug: ProblemSetSlug,
  request: AddProblemToProblemSetRequest,
): Promise<ProblemSetDetail> {
  return postJson(
    `/api/problem-sets/${problemSetSlugValue(problemSetSlug)}/problems`,
    fromProblemSetDetailContract,
    toAddProblemToProblemSetRequestContract(request),
  )
}
