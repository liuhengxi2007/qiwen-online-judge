import { fromUserIdentityContract } from '@/features/user/http/codec'
import { parseProblemId, parseProblemSlug, parseProblemTitle, problemSlugValue } from '@/features/problem/domain/problem'
import type { CreateSubmissionRequest } from '@/features/submission/http/request/CreateSubmissionRequest'
import type { SubmissionDetail } from '@/features/submission/http/response/SubmissionDetail'
import type { SubmissionListRequest } from '@/features/submission/http/request/SubmissionListRequest'
import type { SubmissionListResponse } from '@/features/submission/http/response/SubmissionListResponse'
import type { SubmissionSummary } from '@/features/submission/http/response/SubmissionSummary'
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
import type { JudgeResult } from '@/features/submission/model/JudgeResult'

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

type SubmissionLanguageContract = 'cpp17' | 'python3'
type SubmissionStatusContract = 'queued' | 'running' | 'completed' | 'failed'
type SubmissionVerdictContract =
  | 'accepted'
  | 'wrong_answer'
  | 'compile_error'
  | 'runtime_error'
  | 'time_limit_exceeded'
  | 'system_error'
type SubmissionVerdictFilterContract = 'all' | 'pending' | SubmissionVerdictContract
type SubmissionSortContract = 'submitted' | 'time' | 'memory' | 'code_length'
type SubmissionSortDirectionContract = 'asc' | 'desc'

type CreateSubmissionRequestContract = {
  problemSlug: string
  language: SubmissionLanguageContract
  sourceCode: string
}

type SubmissionListRequestContract = {
  userQuery: string | null
  problemQuery: string | null
  verdict: SubmissionVerdictFilterContract
  sort: SubmissionSortContract
  direction: SubmissionSortDirectionContract
  page: number
  pageSize: number
}

type SubmissionSummaryContract = {
  id: number
  problemId: string
  problemSlug: string
  problemTitle: string
  canViewDetail: boolean
  submitter: UserIdentityContract
  language: SubmissionLanguageContract
  status: SubmissionStatusContract
  verdict: SubmissionVerdictContract | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
  score: number | null
  codeLength: number
  submittedAt: string
  startedAt: string | null
  finishedAt: string | null
}

type SubmissionDetailContract = {
  id: number
  problemId: string
  problemSlug: string
  problemTitle: string
  canManage: boolean
  submitter: UserIdentityContract
  language: SubmissionLanguageContract
  status: SubmissionStatusContract
  verdict: SubmissionVerdictContract | null
  judgeMessage: string | null
  timeUsedMs: number | null
  memoryUsedKb: number | null
  score: number | null
  judgeResult: JudgeResult | null
  codeLength: number
  sourceCode: string
  submittedAt: string
  startedAt: string | null
  finishedAt: string | null
}

type SubmissionListResponseContract = PageResponseContract<SubmissionSummaryContract>

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
    score: submission.score,
    judgeResult: submission.judgeResult,
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
    score: submission.score,
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
