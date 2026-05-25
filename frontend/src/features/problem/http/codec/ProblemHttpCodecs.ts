import type { CreateProblemRequest } from '@/features/problem/http/request/CreateProblemRequest'
import type { ProblemDataUploadResult } from '@/features/problem/http/response/ProblemDataUploadResult'
import type { ProblemDetail } from '@/features/problem/http/response/ProblemDetail'
import type { ProblemListRequest } from '@/features/problem/http/request/ProblemListRequest'
import type { ProblemListResponse } from '@/features/problem/http/response/ProblemListResponse'
import type { ProblemSuggestion } from '@/features/problem/http/response/ProblemSuggestion'
import type { ProblemSummary } from '@/features/problem/http/response/ProblemSummary'
import type { UpdateProblemRequest } from '@/features/problem/http/request/UpdateProblemRequest'
import {
  fromOthersSubmissionAccessContract,
  fromProblemDataContract,
  fromProblemIdContract,
  fromProblemSlugContract,
  fromProblemSpaceLimitMbContract,
  fromProblemStatementTextContract,
  fromProblemTimeLimitMsContract,
  fromProblemTitleContract,
  toOthersSubmissionAccessContract,
  toProblemSlugContract,
  toProblemSpaceLimitMbContract,
  toProblemStatementTextContract,
  toProblemTimeLimitMsContract,
  toProblemTitleContract,
  type OthersSubmissionAccessContract,
} from '@/features/problem/http/codec/ProblemModelHttpCodecs'
import { problemSearchQueryValue } from '@/features/problem/lib/problem-parsers'
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

type ProblemSummaryContract = {
  id: string
  slug: string
  title: string
  data: string | null
  ready: boolean
  timeLimitMs: number
  spaceLimitMb: number
  accessPolicy: ResourceAccessPolicyContract
  othersSubmissionAccess: OthersSubmissionAccessContract
  creator: UserIdentityContract
  createdAt: string
  updatedAt: string
}

type ProblemSuggestionContract = {
  slug: string
  title: string
}

type ProblemListRequestContract = {
  query: string | null
  page: number
  pageSize: number
}

type ProblemDetailContract = {
  id: string
  slug: string
  title: string
  statement: string
  data: string | null
  ready: boolean
  timeLimitMs: number
  spaceLimitMb: number
  accessPolicy: ResourceAccessPolicyContract
  othersSubmissionAccess: OthersSubmissionAccessContract
  creator: UserIdentityContract
  canManage: boolean
  createdAt: string
  updatedAt: string
}

type ProblemDataUploadResultContract = {
  problem: ProblemDetailContract
  uploadedFileCount: number
}

type CreateProblemRequestContract = {
  slug: string
  title: string
  statement: string
  timeLimitMs: number
  spaceLimitMb: number
  accessPolicy: ResourceAccessPolicyContract
  othersSubmissionAccess: OthersSubmissionAccessContract
}

type UpdateProblemRequestContract = {
  title: string
  statement: string
  timeLimitMs: number
  spaceLimitMb: number
  accessPolicy: ResourceAccessPolicyContract
  othersSubmissionAccess: OthersSubmissionAccessContract
}

type ProblemListResponseContract = PageResponseContract<ProblemSummaryContract>

export function fromProblemSummaryContract(problem: ProblemSummaryContract): ProblemSummary {
  return {
    id: fromProblemIdContract(problem.id, 'problem summary id'),
    slug: fromProblemSlugContract(problem.slug, 'problem summary slug'),
    title: fromProblemTitleContract(problem.title, 'problem summary title'),
    data: fromProblemDataContract(problem.data, 'problem summary data'),
    ready: problem.ready,
    timeLimitMs: fromProblemTimeLimitMsContract(problem.timeLimitMs, 'problem summary time limit'),
    spaceLimitMb: fromProblemSpaceLimitMbContract(problem.spaceLimitMb, 'problem summary space limit'),
    accessPolicy: fromResourceAccessPolicyContract(problem.accessPolicy),
    othersSubmissionAccess: fromOthersSubmissionAccessContract(problem.othersSubmissionAccess),
    creator: fromUserIdentityContract(problem.creator),
    createdAt: problem.createdAt,
    updatedAt: problem.updatedAt,
  }
}

export function fromProblemDetailContract(problem: ProblemDetailContract): ProblemDetail {
  return {
    id: fromProblemIdContract(problem.id, 'problem detail id'),
    slug: fromProblemSlugContract(problem.slug, 'problem detail slug'),
    title: fromProblemTitleContract(problem.title, 'problem detail title'),
    statement: fromProblemStatementTextContract(problem.statement, 'problem detail statement'),
    data: fromProblemDataContract(problem.data, 'problem detail data'),
    ready: problem.ready,
    timeLimitMs: fromProblemTimeLimitMsContract(problem.timeLimitMs, 'problem detail time limit'),
    spaceLimitMb: fromProblemSpaceLimitMbContract(problem.spaceLimitMb, 'problem detail space limit'),
    accessPolicy: fromResourceAccessPolicyContract(problem.accessPolicy),
    othersSubmissionAccess: fromOthersSubmissionAccessContract(problem.othersSubmissionAccess),
    creator: fromUserIdentityContract(problem.creator),
    canManage: problem.canManage,
    createdAt: problem.createdAt,
    updatedAt: problem.updatedAt,
  }
}

export function fromProblemDataUploadResultContract(
  result: ProblemDataUploadResultContract,
): ProblemDataUploadResult {
  return {
    problem: fromProblemDetailContract(result.problem),
    uploadedFileCount: result.uploadedFileCount,
  }
}

export function fromProblemListResponseContract(response: ProblemListResponseContract): ProblemListResponse {
  return {
    items: response.items.map(fromProblemSummaryContract),
    page: response.page,
    pageSize: response.pageSize,
    totalItems: response.totalItems,
  }
}

export function fromProblemSuggestionContract(problem: ProblemSuggestionContract): ProblemSuggestion {
  return {
    slug: fromProblemSlugContract(problem.slug, 'problem suggestion slug'),
    title: fromProblemTitleContract(problem.title, 'problem suggestion title'),
  }
}

export function toProblemListRequestContract(request: ProblemListRequest): ProblemListRequestContract {
  return {
    query: request.query ? problemSearchQueryValue(request.query) : null,
    page: request.pageRequest.page,
    pageSize: request.pageRequest.pageSize,
  }
}

export function toCreateProblemRequestContract(request: CreateProblemRequest): CreateProblemRequestContract {
  return {
    slug: toProblemSlugContract(request.slug),
    title: toProblemTitleContract(request.title),
    statement: toProblemStatementTextContract(request.statement),
    timeLimitMs: toProblemTimeLimitMsContract(request.timeLimitMs),
    spaceLimitMb: toProblemSpaceLimitMbContract(request.spaceLimitMb),
    accessPolicy: toResourceAccessPolicyContract(request.accessPolicy),
    othersSubmissionAccess: toOthersSubmissionAccessContract(request.othersSubmissionAccess),
  }
}

export function toUpdateProblemRequestContract(request: UpdateProblemRequest): UpdateProblemRequestContract {
  return {
    title: toProblemTitleContract(request.title),
    statement: toProblemStatementTextContract(request.statement),
    timeLimitMs: toProblemTimeLimitMsContract(request.timeLimitMs),
    spaceLimitMb: toProblemSpaceLimitMbContract(request.spaceLimitMb),
    accessPolicy: toResourceAccessPolicyContract(request.accessPolicy),
    othersSubmissionAccess: toOthersSubmissionAccessContract(request.othersSubmissionAccess),
  }
}
