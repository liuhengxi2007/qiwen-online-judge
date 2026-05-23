import { fromUserIdentityContract } from '@/features/user/http/codec'
import { parseProblemId, parseProblemSlug, parseProblemTitle, problemSlugValue } from '@/features/problem/lib/problem-parsers'
import type { AddProblemToProblemSetRequest } from '@/features/problemset/http/request/AddProblemToProblemSetRequest'
import type { CreateProblemSetRequest } from '@/features/problemset/http/request/CreateProblemSetRequest'
import type { ProblemSetDetail } from '@/features/problemset/http/response/ProblemSetDetail'
import type { ProblemSetListResponse } from '@/features/problemset/http/response/ProblemSetListResponse'
import type { ProblemSetProblemSummary } from '@/features/problemset/model/ProblemSetProblemSummary'
import type { ProblemSetSummary } from '@/features/problemset/http/response/ProblemSetSummary'
import type { UpdateProblemSetRequest } from '@/features/problemset/http/request/UpdateProblemSetRequest'
import {
  parseProblemSetDescription,
  parseProblemSetId,
  parseProblemSetProblemPosition,
  parseProblemSetSlug,
  parseProblemSetTitle,
  problemSetDescriptionValue,
  problemSetSlugValue,
  problemSetTitleValue,
  requireParsed,
} from '@/features/problemset/lib/problemset-parsers'
import {
  fromResourceAccessPolicyContract,
  toResourceAccessPolicyContract,
} from '@/shared/access/resource-access-policy-codec'

type ResourceAccessPolicyContract = ReturnType<typeof toResourceAccessPolicyContract>

type PageResponseContract<TItem> = {
  items: TItem[]
  page: number
  pageSize: number
  totalItems: number
}

type UserIdentityContract = {
  username: string
  displayName: string
}

type ProblemSetProblemSummaryContract = {
  id: string
  slug: string
  title: string
  position: number
}

type ProblemSetSummaryContract = {
  id: string
  slug: string
  title: string
  description: string
  accessPolicy: ResourceAccessPolicyContract
  creator: UserIdentityContract
  createdAt: string
  updatedAt: string
}

type ProblemSetDetailContract = {
  id: string
  slug: string
  title: string
  description: string
  problems: ProblemSetProblemSummaryContract[]
  accessPolicy: ResourceAccessPolicyContract
  creator: UserIdentityContract
  createdAt: string
  updatedAt: string
}

type CreateProblemSetRequestContract = {
  slug: string
  title: string
  description: string
  accessPolicy: ResourceAccessPolicyContract
}

type UpdateProblemSetRequestContract = {
  title: string
  description: string
  accessPolicy: ResourceAccessPolicyContract
}

type AddProblemToProblemSetRequestContract = {
  problemSlug: string
}

type ProblemSetListResponseContract = PageResponseContract<ProblemSetSummaryContract>

export function fromProblemSetProblemSummaryContract(
  problem: ProblemSetProblemSummaryContract,
): ProblemSetProblemSummary {
  return {
    id: requireParsed(parseProblemId(problem.id), 'problem set problem id'),
    slug: requireParsed(parseProblemSlug(problem.slug), 'problem set problem slug'),
    title: requireParsed(parseProblemTitle(problem.title), 'problem set problem title'),
    position: requireParsed(parseProblemSetProblemPosition(problem.position), 'problem set problem position'),
  }
}

export function fromProblemSetSummaryContract(problemSet: ProblemSetSummaryContract): ProblemSetSummary {
  return {
    id: requireParsed(parseProblemSetId(problemSet.id), 'problem set summary id'),
    slug: requireParsed(parseProblemSetSlug(problemSet.slug), 'problem set summary slug'),
    title: requireParsed(parseProblemSetTitle(problemSet.title), 'problem set summary title'),
    description: requireParsed(parseProblemSetDescription(problemSet.description), 'problem set summary description'),
    accessPolicy: fromResourceAccessPolicyContract(problemSet.accessPolicy),
    creator: fromUserIdentityContract(problemSet.creator),
    createdAt: problemSet.createdAt,
    updatedAt: problemSet.updatedAt,
  }
}

export function fromProblemSetDetailContract(problemSet: ProblemSetDetailContract): ProblemSetDetail {
  return {
    id: requireParsed(parseProblemSetId(problemSet.id), 'problem set detail id'),
    slug: requireParsed(parseProblemSetSlug(problemSet.slug), 'problem set detail slug'),
    title: requireParsed(parseProblemSetTitle(problemSet.title), 'problem set detail title'),
    description: requireParsed(parseProblemSetDescription(problemSet.description), 'problem set detail description'),
    problems: problemSet.problems.map(fromProblemSetProblemSummaryContract),
    accessPolicy: fromResourceAccessPolicyContract(problemSet.accessPolicy),
    creator: fromUserIdentityContract(problemSet.creator),
    createdAt: problemSet.createdAt,
    updatedAt: problemSet.updatedAt,
  }
}

export function fromProblemSetListResponseContract(
  response: ProblemSetListResponseContract,
): ProblemSetListResponse {
  return {
    items: response.items.map(fromProblemSetSummaryContract),
    page: response.page,
    pageSize: response.pageSize,
    totalItems: response.totalItems,
  }
}

export function toCreateProblemSetRequestContract(
  request: CreateProblemSetRequest,
): CreateProblemSetRequestContract {
  return {
    slug: problemSetSlugValue(request.slug),
    title: problemSetTitleValue(request.title),
    description: problemSetDescriptionValue(request.description),
    accessPolicy: toResourceAccessPolicyContract(request.accessPolicy),
  }
}

export function toUpdateProblemSetRequestContract(
  request: UpdateProblemSetRequest,
): UpdateProblemSetRequestContract {
  return {
    title: problemSetTitleValue(request.title),
    description: problemSetDescriptionValue(request.description),
    accessPolicy: toResourceAccessPolicyContract(request.accessPolicy),
  }
}

export function toAddProblemToProblemSetRequestContract(
  request: AddProblemToProblemSetRequest,
): AddProblemToProblemSetRequestContract {
  return {
    problemSlug: problemSlugValue(request.problemSlug),
  }
}
