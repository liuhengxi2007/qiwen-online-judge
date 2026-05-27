import type { SubmissionId } from '@/objects/submission/SubmissionId'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import type { SubmissionSourceCode } from '@/objects/submission/SubmissionSourceCode'
import type { SubmissionStatus } from '@/objects/submission/SubmissionStatus'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'
import {
  parseSubmissionId,
  parseSubmissionSourceCode,
  requireParsed,
  submissionSourceCodeValue,
} from '@/objects/submission/submission-parsers'

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
