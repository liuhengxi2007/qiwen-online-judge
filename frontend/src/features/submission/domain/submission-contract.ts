import type {
  CreateSubmissionRequest as CreateSubmissionRequestContract,
  SubmissionDetail as SubmissionDetailContract,
  SubmissionListResponse as SubmissionListResponseContract,
  SubmissionSummary as SubmissionSummaryContract,
} from '@contracts/submission'
import { parseUsername } from '@/features/auth/domain/auth'
import { parseProblemId, parseProblemSlug, problemSlugValue } from '@/features/problem/domain/problem'
import type { CreateSubmissionRequest } from '@/features/submission/model/CreateSubmissionRequest'
import type { SubmissionDetail } from '@/features/submission/model/SubmissionDetail'
import type { SubmissionListResponse } from '@/features/submission/model/SubmissionListResponse'
import type { SubmissionSummary } from '@/features/submission/model/SubmissionSummary'
import {
  parseSubmissionId,
  parseSubmissionSourceCode,
  requireParsed,
  submissionSourceCodeValue,
} from '@/features/submission/domain/submission-parsers'

export function fromSubmissionDetailContract(submission: SubmissionDetailContract): SubmissionDetail {
  return {
    id: requireParsed(parseSubmissionId(submission.id), 'submission id'),
    problemId: requireParsed(parseProblemId(submission.problemId), 'submission problem id'),
    problemSlug: requireParsed(parseProblemSlug(submission.problemSlug), 'submission problem slug'),
    submitterUsername: requireParsed(parseUsername(submission.submitterUsername), 'submission submitter username'),
    language: submission.language,
    status: submission.status,
    verdict: submission.verdict,
    judgeMessage: submission.judgeMessage,
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
    submitterUsername: requireParsed(parseUsername(submission.submitterUsername), 'submission submitter username'),
    language: submission.language,
    status: submission.status,
    verdict: submission.verdict,
    submittedAt: submission.submittedAt,
    startedAt: submission.startedAt,
    finishedAt: submission.finishedAt,
  }
}

export function fromSubmissionListResponseContract(response: SubmissionListResponseContract): SubmissionListResponse {
  return response.map(fromSubmissionSummaryContract)
}

export function toCreateSubmissionRequestContract(request: CreateSubmissionRequest): CreateSubmissionRequestContract {
  return {
    problemSlug: problemSlugValue(request.problemSlug),
    language: request.language,
    sourceCode: submissionSourceCodeValue(request.sourceCode),
  }
}
