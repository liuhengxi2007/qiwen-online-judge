import type { AddProblemToProblemSetRequest } from '@/objects/problemset/request/AddProblemToProblemSetRequest'
import type { CreateProblemSetRequest } from '@/objects/problemset/request/CreateProblemSetRequest'
import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import type { ProblemSetListResponse } from '@/objects/problemset/response/ProblemSetListResponse'
import type { ProblemSetSummary } from '@/objects/problemset/response/ProblemSetSummary'
import type { UpdateProblemSetRequest } from '@/objects/problemset/request/UpdateProblemSetRequest'
import { toProblemSlugContract } from '@/objects/problem/ProblemSlug'
import {
  fromProblemSetDescriptionContract,
  toProblemSetDescriptionContract,
} from '@/objects/problemset/ProblemSetDescription'
import { fromProblemSetIdContract } from '@/objects/problemset/ProblemSetId'
import { fromProblemSetProblemSummaryContract } from '@/objects/problemset/ProblemSetProblemSummary'
import { fromProblemSetSlugContract, toProblemSetSlugContract } from '@/objects/problemset/ProblemSetSlug'
import { fromProblemSetTitleContract, toProblemSetTitleContract } from '@/objects/problemset/ProblemSetTitle'
import {
  fromResourceAccessPolicyContract,
  toResourceAccessPolicyContract,
} from '@/objects/shared/access/ResourceAccessPolicy'
import { fromUserIdentityContract } from '@/objects/user/UserIdentity'

type ProblemSetProblemSummaryContract = {
  id: string
  slug: string
  title: string
  position: number
}

type UserIdentityContract = {
  username: string
  displayName: string
}

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
