import type {
  AddProblemToProblemSetRequest,
  ProblemSetDetail,
  ProblemSetSlug,
} from '@/features/problemset/domain/problemset'
import {
  fromProblemSetDetailContract,
  problemSetSlugValue,
  toAddProblemToProblemSetRequestContract,
} from '@/features/problemset/domain/problemset'
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
