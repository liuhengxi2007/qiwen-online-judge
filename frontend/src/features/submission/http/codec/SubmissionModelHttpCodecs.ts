import type { SubmissionId } from '@/features/submission/model/SubmissionId'
import type { SubmissionLanguage } from '@/features/submission/model/SubmissionLanguage'
import type { SubmissionSourceCode } from '@/features/submission/model/SubmissionSourceCode'
import type { SubmissionStatus } from '@/features/submission/model/SubmissionStatus'
import type { SubmissionVerdict } from '@/features/submission/model/SubmissionVerdict'
import {
  parseSubmissionId,
  parseSubmissionSourceCode,
  requireParsed,
  submissionSourceCodeValue,
} from '@/features/submission/lib/submission-parsers'

export type SubmissionIdContract = number
export type SubmissionLanguageContract = 'cpp17' | 'python3'
export type SubmissionStatusContract = 'queued' | 'running' | 'completed' | 'failed'
export type SubmissionVerdictContract =
  | 'accepted'
  | 'wrong_answer'
  | 'compile_error'
  | 'runtime_error'
  | 'time_limit_exceeded'
  | 'system_error'
export type SubmissionSourceCodeContract = string

export function fromSubmissionIdContract(value: SubmissionIdContract, label: string): SubmissionId {
  return requireParsed(parseSubmissionId(value), label)
}

export function fromSubmissionLanguageContract(value: SubmissionLanguageContract): SubmissionLanguage {
  return value
}

export function toSubmissionLanguageContract(value: SubmissionLanguage): SubmissionLanguageContract {
  return value
}

export function fromSubmissionStatusContract(value: SubmissionStatusContract): SubmissionStatus {
  return value
}

export function fromSubmissionVerdictContract(value: SubmissionVerdictContract): SubmissionVerdict {
  return value
}

export function fromSubmissionSourceCodeContract(
  value: SubmissionSourceCodeContract,
  label: string,
): SubmissionSourceCode {
  return requireParsed(parseSubmissionSourceCode(value), label)
}

export function toSubmissionSourceCodeContract(value: SubmissionSourceCode): SubmissionSourceCodeContract {
  return submissionSourceCodeValue(value)
}
