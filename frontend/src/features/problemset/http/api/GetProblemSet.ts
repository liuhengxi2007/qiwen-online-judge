import type {
  ProblemSetDetail,
  ProblemSetSlug,
} from '@/features/problemset/domain/problemset'
import { problemSetSlugValue } from '@/features/problemset/domain/problemset'
import { fromProblemSetDetailContract } from '@/features/problemset/http/codec'
import { requestJson } from '@/shared/api/http-client'

export async function getProblemSet(problemSetSlug: ProblemSetSlug): Promise<ProblemSetDetail> {
  return requestJson(`/api/problem-sets/${problemSetSlugValue(problemSetSlug)}`, fromProblemSetDetailContract)
}
