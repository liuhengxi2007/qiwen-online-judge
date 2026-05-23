import type { ProblemSetDetail } from '@/features/problemset/http/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/features/problemset/model/ProblemSetSlug'
import { problemSetSlugValue } from '@/features/problemset/lib/problemset-parsers'
import { fromProblemSetDetailContract } from '@/features/problemset/http/codec'
import { requestJson } from '@/shared/api/http-client'

export async function getProblemSet(problemSetSlug: ProblemSetSlug): Promise<ProblemSetDetail> {
  return requestJson(`/api/problem-sets/${problemSetSlugValue(problemSetSlug)}`, fromProblemSetDetailContract)
}
