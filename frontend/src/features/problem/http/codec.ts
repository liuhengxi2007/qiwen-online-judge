import { fromUserIdentityContract } from '@/features/user/http/codec'
import type { CreateProblemRequest } from '@/features/problem/http/request/CreateProblemRequest'
import type { ProblemData } from '@/features/problem/model/ProblemData'
import type { ProblemDetail } from '@/features/problem/http/response/ProblemDetail'
import type { ProblemDataUploadResult } from '@/features/problem/http/response/ProblemDataUploadResult'
import type { ProblemListRequest } from '@/features/problem/http/request/ProblemListRequest'
import type { ProblemListResponse } from '@/features/problem/http/response/ProblemListResponse'
import type { ProblemSuggestion } from '@/features/problem/http/response/ProblemSuggestion'
import type { ProblemSummary } from '@/features/problem/http/response/ProblemSummary'
import type { UpdateProblemRequest } from '@/features/problem/http/request/UpdateProblemRequest'
import {
  parseProblemDataFilename,
  parseProblemId,
  parseProblemSlug,
  parseProblemSpaceLimitMb,
  parseProblemStatementText,
  parseProblemTimeLimitMs,
  parseProblemTitle,
  problemSearchQueryValue,
  problemSlugValue,
  problemSpaceLimitMbValue,
  problemStatementTextValue,
  problemTimeLimitMsValue,
  problemTitleValue,
  requireParsed,
} from '@/features/problem/domain/problem-parsers'
import type { ResourceAccessPolicy } from '@/shared/access/AccessPolicy'

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

type OthersSubmissionAccessContract = 'none' | 'summary' | 'detail'

type ProblemSummaryContract = {
  id: string
  slug: string
  title: string
  data: string | null
  ready: boolean
  timeLimitMs: number
  spaceLimitMb: number
  accessPolicy: ResourceAccessPolicy
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
  accessPolicy: ResourceAccessPolicy
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
  accessPolicy: ResourceAccessPolicy
  othersSubmissionAccess: OthersSubmissionAccessContract
}

type UpdateProblemRequestContract = {
  title: string
  statement: string
  timeLimitMs: number
  spaceLimitMb: number
  accessPolicy: ResourceAccessPolicy
  othersSubmissionAccess: OthersSubmissionAccessContract
}

type ProblemListResponseContract = PageResponseContract<ProblemSummaryContract>

function fromProblemDataContract(rawData: string | null, label: string): ProblemData {
  return {
    value: rawData === null ? null : requireParsed(parseProblemDataFilename(rawData), label),
  }
}

export function fromProblemSummaryContract(problem: ProblemSummaryContract): ProblemSummary {
  return {
    id: requireParsed(parseProblemId(problem.id), 'problem summary id'),
    slug: requireParsed(parseProblemSlug(problem.slug), 'problem summary slug'),
    title: requireParsed(parseProblemTitle(problem.title), 'problem summary title'),
    data: fromProblemDataContract(problem.data, 'problem summary data'),
    ready: problem.ready,
    timeLimitMs: requireParsed(parseProblemTimeLimitMs(problem.timeLimitMs), 'problem summary time limit'),
    spaceLimitMb: requireParsed(parseProblemSpaceLimitMb(problem.spaceLimitMb), 'problem summary space limit'),
    accessPolicy: problem.accessPolicy,
    othersSubmissionAccess: problem.othersSubmissionAccess,
    creator: fromUserIdentityContract(problem.creator),
    createdAt: problem.createdAt,
    updatedAt: problem.updatedAt,
  }
}

export function fromProblemDetailContract(problem: ProblemDetailContract): ProblemDetail {
  return {
    id: requireParsed(parseProblemId(problem.id), 'problem detail id'),
    slug: requireParsed(parseProblemSlug(problem.slug), 'problem detail slug'),
    title: requireParsed(parseProblemTitle(problem.title), 'problem detail title'),
    statement: requireParsed(parseProblemStatementText(problem.statement), 'problem detail statement'),
    data: fromProblemDataContract(problem.data, 'problem detail data'),
    ready: problem.ready,
    timeLimitMs: requireParsed(parseProblemTimeLimitMs(problem.timeLimitMs), 'problem detail time limit'),
    spaceLimitMb: requireParsed(parseProblemSpaceLimitMb(problem.spaceLimitMb), 'problem detail space limit'),
    accessPolicy: problem.accessPolicy,
    othersSubmissionAccess: problem.othersSubmissionAccess,
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
    slug: requireParsed(parseProblemSlug(problem.slug), 'problem suggestion slug'),
    title: requireParsed(parseProblemTitle(problem.title), 'problem suggestion title'),
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
    slug: problemSlugValue(request.slug),
    title: problemTitleValue(request.title),
    statement: problemStatementTextValue(request.statement),
    timeLimitMs: problemTimeLimitMsValue(request.timeLimitMs),
    spaceLimitMb: problemSpaceLimitMbValue(request.spaceLimitMb),
    accessPolicy: request.accessPolicy,
    othersSubmissionAccess: request.othersSubmissionAccess,
  }
}

export function toUpdateProblemRequestContract(request: UpdateProblemRequest): UpdateProblemRequestContract {
  return {
    title: problemTitleValue(request.title),
    statement: problemStatementTextValue(request.statement),
    timeLimitMs: problemTimeLimitMsValue(request.timeLimitMs),
    spaceLimitMb: problemSpaceLimitMbValue(request.spaceLimitMb),
    accessPolicy: request.accessPolicy,
    othersSubmissionAccess: request.othersSubmissionAccess,
  }
}
