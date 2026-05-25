import type { AddProblemToProblemSetRequest } from '@/features/problemset/model/request/AddProblemToProblemSetRequest'
import type { CreateProblemSetRequest } from '@/features/problemset/model/request/CreateProblemSetRequest'
import type { ProblemSetDetail } from '@/features/problemset/model/response/ProblemSetDetail'
import type { ProblemSetListResponse } from '@/features/problemset/model/response/ProblemSetListResponse'
import type { ProblemSetSummary } from '@/features/problemset/model/response/ProblemSetSummary'
import type { UpdateProblemSetRequest } from '@/features/problemset/model/request/UpdateProblemSetRequest'
import { toProblemSlugContract } from '@/features/problem/http/codec/ProblemModelHttpCodecs'
import {
  fromProblemSetDescriptionContract,
  fromProblemSetIdContract,
  fromProblemSetProblemSummaryContract,
  fromProblemSetSlugContract,
  fromProblemSetTitleContract,
  toProblemSetDescriptionContract,
  toProblemSetSlugContract,
  toProblemSetTitleContract,
  type ProblemSetProblemSummaryContract,
} from '@/features/problemset/http/codec/ProblemSetModelHttpCodecs'
import {
  fromResourceAccessPolicyContract,
  toResourceAccessPolicyContract,
} from '@/shared/domain/access/resource-access-policy-codec'
import {
  fromUserIdentityContract,
  type UserIdentityContract,
} from '@/features/user/http/codec/UserModelHttpCodecs'

type ResourceAccessPolicyContract = ReturnType<typeof toResourceAccessPolicyContract>

type PageResponseContract<TItem> = {
  items: TItem[]
  page: number
  pageSize: number
  totalItems: number
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

export function fromProblemSetSummaryContract(problemSet: ProblemSetSummaryContract): ProblemSetSummary {
  return {
    id: fromProblemSetIdContract(problemSet.id, 'problem set summary id'),
    slug: fromProblemSetSlugContract(problemSet.slug, 'problem set summary slug'),
    title: fromProblemSetTitleContract(problemSet.title, 'problem set summary title'),
    description: fromProblemSetDescriptionContract(problemSet.description, 'problem set summary description'),
    accessPolicy: fromResourceAccessPolicyContract(problemSet.accessPolicy),
    creator: fromUserIdentityContract(problemSet.creator),
    createdAt: problemSet.createdAt,
    updatedAt: problemSet.updatedAt,
  }
}

export function fromProblemSetDetailContract(problemSet: ProblemSetDetailContract): ProblemSetDetail {
  return {
    id: fromProblemSetIdContract(problemSet.id, 'problem set detail id'),
    slug: fromProblemSetSlugContract(problemSet.slug, 'problem set detail slug'),
    title: fromProblemSetTitleContract(problemSet.title, 'problem set detail title'),
    description: fromProblemSetDescriptionContract(problemSet.description, 'problem set detail description'),
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
    slug: toProblemSetSlugContract(request.slug),
    title: toProblemSetTitleContract(request.title),
    description: toProblemSetDescriptionContract(request.description),
    accessPolicy: toResourceAccessPolicyContract(request.accessPolicy),
  }
}

export function toUpdateProblemSetRequestContract(
  request: UpdateProblemSetRequest,
): UpdateProblemSetRequestContract {
  return {
    title: toProblemSetTitleContract(request.title),
    description: toProblemSetDescriptionContract(request.description),
    accessPolicy: toResourceAccessPolicyContract(request.accessPolicy),
  }
}

export function toAddProblemToProblemSetRequestContract(
  request: AddProblemToProblemSetRequest,
): AddProblemToProblemSetRequestContract {
  return {
    problemSlug: toProblemSlugContract(request.problemSlug),
  }
}
