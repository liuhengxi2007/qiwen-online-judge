export type { ParseResult } from '@/features/submission/domain/submission-parsers'
export {
  isSubmissionLanguage,
  isSubmissionStatus,
  isSubmissionVerdict,
  isTerminalSubmissionStatus,
  parseSubmissionId,
  parseSubmissionSourceCode,
  submissionIdValue,
  submissionLanguageLabel,
  submissionSourceCodeValue,
  submissionStatusLabel,
  submissionVerdictLabel,
} from '@/features/submission/domain/submission-parsers'
export {
  fromSubmissionDetailContract,
  fromSubmissionListResponseContract,
  fromSubmissionSummaryContract,
  toCreateSubmissionRequestContract,
} from '@/features/submission/domain/submission-contract'

export type { CreateSubmissionRequest } from '@/features/submission/model/CreateSubmissionRequest'
export type { SubmissionDetail } from '@/features/submission/model/SubmissionDetail'
export type { SubmissionId } from '@/features/submission/model/SubmissionId'
export type { SubmissionLanguage } from '@/features/submission/model/SubmissionLanguage'
export type { SubmissionListResponse } from '@/features/submission/model/SubmissionListResponse'
export type { SubmissionSourceCode } from '@/features/submission/model/SubmissionSourceCode'
export type { SubmissionStatus } from '@/features/submission/model/SubmissionStatus'
export type { SubmissionSummary } from '@/features/submission/model/SubmissionSummary'
export type { SubmissionVerdict } from '@/features/submission/model/SubmissionVerdict'
