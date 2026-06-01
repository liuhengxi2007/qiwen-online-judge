import type { UserIdentity } from '@/objects/user/UserIdentity'
import { fromUserIdentityContract } from '@/objects/user/UserIdentity'
import type { ProblemId } from '@/objects/problem/ProblemId'
import { fromProblemIdContract } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { fromProblemSlugContract } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import { fromProblemTitleContract } from '@/objects/problem/ProblemTitle'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { fromSubmissionIdContract } from '@/objects/submission/SubmissionId'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import { fromSubmissionLanguageContract } from '@/objects/submission/SubmissionLanguage'
import type { SubmissionStatus } from '@/objects/submission/SubmissionStatus'
import { fromSubmissionStatusContract } from '@/objects/submission/SubmissionStatus'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'
import { fromSubmissionVerdictContract } from '@/objects/submission/SubmissionVerdict'
import { readBoolean, readNonNegativeSafeInteger, readNullable, readNumber, readRecord, readSafeInteger, readString } from '@/objects/shared/PageResponse'

export type SubmissionSummary = {
  id: SubmissionId
  problemId: ProblemId
  problemSlug: ProblemSlug
  problemTitle: ProblemTitle
  canViewDetail: boolean
  submitter: UserIdentity
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

export function fromSubmissionSummaryContract(value: unknown, label: string): SubmissionSummary {
  const submission = readRecord(value, label)
  return {
    id: fromSubmissionIdContract(readSafeInteger(submission.id, `${label} id`), `${label} id`),
    problemId: fromProblemIdContract(readString(submission.problemId, `${label} problem id`), `${label} problem id`),
    problemSlug: fromProblemSlugContract(readString(submission.problemSlug, `${label} problem slug`), `${label} problem slug`),
    problemTitle: fromProblemTitleContract(readString(submission.problemTitle, `${label} problem title`), `${label} problem title`),
    canViewDetail: readBoolean(submission.canViewDetail, `${label} can view detail`),
    submitter: fromUserIdentityContract(submission.submitter, `${label} submitter`),
    language: fromSubmissionLanguageContract(submission.language),
    status: fromSubmissionStatusContract(submission.status),
    verdict: readNullable(submission.verdict, `${label} verdict`, fromSubmissionVerdictContract),
    timeUsedMs: readNullable(submission.timeUsedMs, `${label} time used ms`, readNonNegativeSafeInteger),
    memoryUsedKb: readNullable(submission.memoryUsedKb, `${label} memory used kb`, readNonNegativeSafeInteger),
    score: readNullable(submission.score, `${label} score`, readNumber),
    codeLength: readNonNegativeSafeInteger(submission.codeLength, `${label} code length`),
    submittedAt: readString(submission.submittedAt, `${label} submitted at`),
    startedAt: readNullable(submission.startedAt, `${label} started at`, readString),
    finishedAt: readNullable(submission.finishedAt, `${label} finished at`, readString),
  }
}
