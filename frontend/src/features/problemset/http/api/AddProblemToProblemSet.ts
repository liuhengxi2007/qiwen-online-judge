import type { AddProblemToProblemSetRequest } from '@/features/problemset/http/request/AddProblemToProblemSetRequest'
import type { ProblemSetDetail } from '@/features/problemset/http/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/features/problemset/model/ProblemSetSlug'
import { problemSetSlugValue } from '@/features/problemset/lib/problemset-parsers'
import {
  fromProblemSetDetailContract,
  toAddProblemToProblemSetRequestContract,
} from '@/features/problemset/http/codec/ProblemSetHttpCodecs'
import { postJson } from '@/shared/api/http-client'

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
