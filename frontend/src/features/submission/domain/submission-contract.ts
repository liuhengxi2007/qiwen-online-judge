import type {
  CreateSubmissionRequest as CreateSubmissionRequestContract,
  SubmissionDetail as SubmissionDetailContract,
  SubmissionListRequest as SubmissionListRequestContract,
  SubmissionListResponse as SubmissionListResponseContract,
  SubmissionSummary as SubmissionSummaryContract,
} from '@contracts/submission'
import { fromUserIdentityContract } from '@/features/user/domain/user'
import { parseProblemId, parseProblemSlug, parseProblemTitle, problemSlugValue } from '@/features/problem/domain/problem'
import type { CreateSubmissionRequest } from '@/features/submission/model/CreateSubmissionRequest'
import type { SubmissionDetail } from '@/features/submission/model/SubmissionDetail'
import type { SubmissionListRequest } from '@/features/submission/model/SubmissionListRequest'
import type { SubmissionListResponse } from '@/features/submission/model/SubmissionListResponse'
import type { SubmissionSummary } from '@/features/submission/model/SubmissionSummary'
import {
  isSubmissionSort,
  isSubmissionSortDirection,
  isSubmissionVerdictFilter,
  parseSubmissionId,
  parseSubmissionProblemQuery,
  parseSubmissionSourceCode,
  parseSubmissionUserQuery,
  requireParsed,
  submissionProblemQueryValue,
  submissionSourceCodeValue,
  submissionUserQueryValue,
} from '@/features/submission/domain/submission-parsers'

export function fromSubmissionDetailContract(submission: SubmissionDetailContract): SubmissionDetail {
  return {
    id: requireParsed(parseSubmissionId(submission.id), 'submission id'),
    problemId: requireParsed(parseProblemId(submission.problemId), 'submission problem id'),
    problemSlug: requireParsed(parseProblemSlug(submission.problemSlug), 'submission problem slug'),
    problemTitle: requireParsed(parseProblemTitle(submission.problemTitle), 'submission problem title'),
    canManage: submission.canManage,
    submitter: fromUserIdentityContract(submission.submitter),
    language: submission.language,
    status: submission.status,
    verdict: submission.verdict,
    judgeMessage: submission.judgeMessage,
    timeUsedMs: submission.timeUsedMs,
    memoryUsedKb: submission.memoryUsedKb,
    codeLength: submission.codeLength,
    sourceCode: requireParsed(parseSubmissionSourceCode(submission.sourceCode), 'submission source code'),
    submittedAt: submission.submittedAt,
    startedAt: submission.startedAt,
    finishedAt: submission.finishedAt,
  }
}

export function fromSubmissionSummaryContract(submission: SubmissionSummaryContract): SubmissionSummary {
  return {
    id: requireParsed(parseSubmissionId(submission.id), 'submission id'),
    problemId: requireParsed(parseProblemId(submission.problemId), 'submission problem id'),
    problemSlug: requireParsed(parseProblemSlug(submission.problemSlug), 'submission problem slug'),
    problemTitle: requireParsed(parseProblemTitle(submission.problemTitle), 'submission problem title'),
    canViewDetail: submission.canViewDetail,
    submitter: fromUserIdentityContract(submission.submitter),
    language: submission.language,
    status: submission.status,
    verdict: submission.verdict,
    timeUsedMs: submission.timeUsedMs,
    memoryUsedKb: submission.memoryUsedKb,
    codeLength: submission.codeLength,
    submittedAt: submission.submittedAt,
    startedAt: submission.startedAt,
    finishedAt: submission.finishedAt,
  }
}

export function fromSubmissionListResponseContract(response: SubmissionListResponseContract): SubmissionListResponse {
  return {
    items: response.items.map(fromSubmissionSummaryContract),
    page: response.page,
    pageSize: response.pageSize,
    totalItems: response.totalItems,
  }
}

export function toCreateSubmissionRequestContract(request: CreateSubmissionRequest): CreateSubmissionRequestContract {
  return {
    problemSlug: problemSlugValue(request.problemSlug),
    language: request.language,
    sourceCode: submissionSourceCodeValue(request.sourceCode),
  }
}

export function toSubmissionListRequestContract(request: SubmissionListRequest): SubmissionListRequestContract {
  return {
    userQuery: request.userQuery ? submissionUserQueryValue(request.userQuery) : null,
    problemQuery: request.problemQuery ? submissionProblemQueryValue(request.problemQuery) : null,
    verdict: request.verdict,
    sort: request.sort,
    direction: request.direction,
    page: request.pageRequest.page,
    pageSize: request.pageRequest.pageSize,
  }
}

export function fromSubmissionListRequestContract(request: SubmissionListRequestContract): SubmissionListRequest {
  if (request.userQuery !== null && typeof request.userQuery !== 'string') {
    throw new Error('Invalid submission list request user query.')
  }

  if (request.problemQuery !== null && typeof request.problemQuery !== 'string') {
    throw new Error('Invalid submission list request problem query.')
  }

  if (!isSubmissionVerdictFilter(request.verdict)) {
    throw new Error('Invalid submission list request verdict filter.')
  }

  if (!isSubmissionSort(request.sort)) {
    throw new Error('Invalid submission list request sort.')
  }

  if (!isSubmissionSortDirection(request.direction)) {
    throw new Error('Invalid submission list request sort direction.')
  }

  return {
    userQuery: request.userQuery === null ? null : requireParsed(parseSubmissionUserQuery(request.userQuery), 'submission list request user query'),
    problemQuery:
      request.problemQuery === null
        ? null
        : requireParsed(parseSubmissionProblemQuery(request.problemQuery), 'submission list request problem query'),
    verdict: request.verdict,
    sort: request.sort,
    direction: request.direction,
    pageRequest: {
      page: request.page,
      pageSize: request.pageSize,
    },
  }
}
