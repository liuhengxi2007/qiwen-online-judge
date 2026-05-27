import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { problemSetSlugValue } from '@/objects/problemset/ProblemSetSlug'
import { fromProblemSetDetailContract } from '@/apis/problemset/codecs/ProblemSetHttpCodecs'
import { requestJson } from '@/system/api/http-client'

export async function getProblemSet(problemSetSlug: ProblemSetSlug): Promise<ProblemSetDetail> {
  return requestJson(`/api/problem-sets/${problemSetSlugValue(problemSetSlug)}`, fromProblemSetDetailContract)
}
