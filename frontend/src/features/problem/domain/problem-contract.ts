import type {
  ProblemListRequest as ProblemListRequestContract,
  CreateProblemRequest as CreateProblemRequestContract,
  ProblemDetail as ProblemDetailContract,
  ProblemListResponse as ProblemListResponseContract,
  ProblemSuggestion as ProblemSuggestionContract,
  ProblemSummary as ProblemSummaryContract,
  UpdateProblemRequest as UpdateProblemRequestContract,
} from '@contracts/problem'
import { fromUserIdentityContract } from '@/features/user/domain/user'
import type { CreateProblemRequest } from '@/features/problem/model/CreateProblemRequest'
import type { ProblemDetail } from '@/features/problem/model/ProblemDetail'
import type { ProblemListRequest } from '@/features/problem/model/ProblemListRequest'
import type { ProblemListResponse } from '@/features/problem/model/ProblemListResponse'
import type { ProblemSuggestion } from '@/features/problem/model/ProblemSuggestion'
import type { ProblemSummary } from '@/features/problem/model/ProblemSummary'
import type { UpdateProblemRequest } from '@/features/problem/model/UpdateProblemRequest'
import {
  fromProblemDataContract,
  parseProblemId,
  parseProblemSlug,
  parseProblemSpaceLimitMb,
  parseProblemStatementText,
  parseProblemTimeLimitMs,
  parseProblemTitle,
  problemSlugValue,
  problemSpaceLimitMbValue,
  problemStatementTextValue,
  problemTimeLimitMsValue,
  problemTitleValue,
  requireParsed,
} from '@/features/problem/domain/problem-parsers'

export function fromProblemSummaryContract(problem: ProblemSummaryContract): ProblemSummary {
  return {
    id: requireParsed(parseProblemId(problem.id), 'problem summary id'),
    slug: requireParsed(parseProblemSlug(problem.slug), 'problem summary slug'),
    title: requireParsed(parseProblemTitle(problem.title), 'problem summary title'),
    data: fromProblemDataContract(problem.data, 'problem summary data'),
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
    query: request.query,
    page: request.page,
    pageSize: request.pageSize,
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
