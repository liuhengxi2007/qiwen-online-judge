import type { CreateSubmissionRequest } from '@/objects/submission/request/CreateSubmissionRequest'
import type { JudgeResult } from '@/objects/submission/JudgeResult'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'
import type { SubmissionListRequest } from '@/objects/submission/request/SubmissionListRequest'
import type { SubmissionListResponse } from '@/objects/submission/response/SubmissionListResponse'
import type { SubmissionSummary } from '@/objects/submission/response/SubmissionSummary'
import { fromProblemIdContract } from '@/objects/problem/ProblemId'
import { fromProblemSlugContract, toProblemSlugContract } from '@/objects/problem/ProblemSlug'
import { fromProblemTitleContract } from '@/objects/problem/ProblemTitle'
import {
  fromSubmissionLanguageContract,
  toSubmissionLanguageContract,
  type SubmissionLanguage,
} from '@/objects/submission/SubmissionLanguage'
import { fromSubmissionIdContract } from '@/objects/submission/SubmissionId'
import { fromSubmissionSourceCodeContract, toSubmissionSourceCodeContract } from '@/objects/submission/SubmissionSourceCode'
import { fromSubmissionStatusContract, type SubmissionStatus } from '@/objects/submission/SubmissionStatus'
import { fromSubmissionVerdictContract, type SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'
import {
  fromSubmissionProblemQueryContract,
  submissionProblemQueryValue,
} from '@/objects/submission/request/SubmissionProblemQuery'
import { isSubmissionSort } from '@/objects/submission/request/SubmissionSort'
import { isSubmissionSortDirection } from '@/objects/submission/request/SubmissionSortDirection'
import {
  fromSubmissionUserQueryContract,
  submissionUserQueryValue,
} from '@/objects/submission/request/SubmissionUserQuery'
import { isSubmissionVerdictFilter } from '@/objects/submission/request/SubmissionVerdictFilter'
import { fromUserIdentityContract } from '@/objects/user/UserIdentity'

type UserIdentityContract = {
  username: string
  displayName: string
}

type PageResponseContract<TItem> = {
  items: TItem[]
  page: number
  pageSize: number
  totalItems: number
}

type SubmissionVerdictFilterContract = 'all' | 'pending' | SubmissionVerdict
type SubmissionSortRequestContract = 'submitted' | 'time' | 'memory' | 'code_length'
type SubmissionSortDirectionRequestContract = 'asc' | 'desc'

type CreateSubmissionRequestContract = {
  problemSlug: string
  language: SubmissionLanguage
  sourceCode: string
}

type SubmissionListRequestContract = {
  userQuery: string | null
  problemQuery: string | null
  verdict: SubmissionVerdictFilterContract
  sort: SubmissionSortRequestContract
  direction: SubmissionSortDirectionRequestContract
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
  language: SubmissionLanguage
  status: SubmissionStatus
  verdict: SubmissionVerdict | null
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
  language: SubmissionLanguage
  status: SubmissionStatus
  verdict: SubmissionVerdict | null
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
    id: fromSubmissionIdContract(submission.id, 'submission id'),
    problemId: fromProblemIdContract(submission.problemId, 'submission problem id'),
    problemSlug: fromProblemSlugContract(submission.problemSlug, 'submission problem slug'),
    problemTitle: fromProblemTitleContract(submission.problemTitle, 'submission problem title'),
    canManage: submission.canManage,
    submitter: fromUserIdentityContract(submission.submitter),
    language: fromSubmissionLanguageContract(submission.language),
    status: fromSubmissionStatusContract(submission.status),
    verdict: submission.verdict === null ? null : fromSubmissionVerdictContract(submission.verdict),
    judgeMessage: submission.judgeMessage,
    timeUsedMs: submission.timeUsedMs,
    memoryUsedKb: submission.memoryUsedKb,
    score: submission.score,
    judgeResult: submission.judgeResult,
    codeLength: submission.codeLength,
    sourceCode: fromSubmissionSourceCodeContract(submission.sourceCode, 'submission source code'),
    submittedAt: submission.submittedAt,
    startedAt: submission.startedAt,
    finishedAt: submission.finishedAt,
  }
}

export function fromSubmissionSummaryContract(submission: SubmissionSummaryContract): SubmissionSummary {
  return {
    id: fromSubmissionIdContract(submission.id, 'submission id'),
    problemId: fromProblemIdContract(submission.problemId, 'submission problem id'),
    problemSlug: fromProblemSlugContract(submission.problemSlug, 'submission problem slug'),
    problemTitle: fromProblemTitleContract(submission.problemTitle, 'submission problem title'),
    canViewDetail: submission.canViewDetail,
    submitter: fromUserIdentityContract(submission.submitter),
    language: fromSubmissionLanguageContract(submission.language),
    status: fromSubmissionStatusContract(submission.status),
    verdict: submission.verdict === null ? null : fromSubmissionVerdictContract(submission.verdict),
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
    problemSlug: toProblemSlugContract(request.problemSlug),
    language: toSubmissionLanguageContract(request.language),
    sourceCode: toSubmissionSourceCodeContract(request.sourceCode),
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
    userQuery:
      request.userQuery === null
        ? null
        : fromSubmissionUserQueryContract(request.userQuery, 'submission list request user query'),
    problemQuery:
      request.problemQuery === null
        ? null
        : fromSubmissionProblemQueryContract(request.problemQuery, 'submission list request problem query'),
    verdict: request.verdict,
    sort: request.sort,
    direction: request.direction,
    pageRequest: {
      page: request.page,
      pageSize: request.pageSize,
    },
  }
}
