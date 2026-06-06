import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import type { SubmissionResultDisplayMode } from '@/objects/submission/SubmissionResultDisplayMode'
import type { SubmissionSource } from '@/objects/submission/SubmissionSource'
import type { SubmissionStatus } from '@/objects/submission/SubmissionStatus'
import type { SubmissionVerdict } from '@/objects/submission/SubmissionVerdict'
import type { SubmissionVerdictFilter } from '@/objects/submission/request/SubmissionVerdictFilter'
import { formatOptionalBinarySizeBytes } from '@/system/format/binary-size'
import { formatDateTime, formatUtcOffsetTitle } from '@/system/format/date-time'

export function formatOptionalDurationMs(value: number | null): string {
  if (value === null) {
    return '--'
  }

  return `${value} ms`
}

export function formatOptionalMemoryKb(value: number | null): string {
  return formatOptionalBinarySizeBytes(value === null ? null : value * 1024, '--', { minimumUnit: 'KiB' })
}

export function formatCodeLength(value: number): string {
  return `${value} B`
}

export function formatOptionalScore(value: number | null): string {
  if (value === null) {
    return '--'
  }

  return `${new Intl.NumberFormat(undefined, {
    maximumFractionDigits: 6,
  }).format(value * 100)}`
}

export const formatSubmissionDateTime = formatDateTime

export const formatSubmissionDateTimeTitle = formatUtcOffsetTitle

export function submissionLanguageLabel(language: SubmissionLanguage): string {
  switch (language) {
    case 'cpp17':
      return 'C++17'
    case 'python3':
      return 'Python 3'
  }
}

export function submissionStatusLabel(status: SubmissionStatus): string {
  switch (status) {
    case 'queued':
      return 'Queued'
    case 'running':
      return 'Running'
    case 'completed':
      return 'Completed'
    case 'failed':
      return 'Failed'
  }
}

export function submissionVerdictLabel(verdict: SubmissionVerdict | null): string {
  switch (verdict) {
    case null:
      return 'Pending'
    case 'accepted':
      return 'Accepted'
    case 'accepted_by_protocol':
      return 'Accepted by Protocol'
    case 'wrong_answer':
      return 'Wrong Answer'
    case 'compile_error':
      return 'Compile Error'
    case 'runtime_error':
      return 'Runtime Error'
    case 'time_limit_exceeded':
      return 'Time Limit Exceeded'
    case 'system_error':
      return 'System Error'
  }
}

export function submissionJudgeStateLabel(
  status: SubmissionStatus,
  verdict: SubmissionVerdict | null,
): string {
  if (verdict !== null) {
    return submissionVerdictLabel(verdict)
  }

  switch (status) {
    case 'queued':
    case 'running':
      return submissionVerdictLabel(null)
    case 'completed':
    case 'failed':
      return submissionStatusLabel(status)
  }
}

export function submissionProblemPath(source: SubmissionSource, problemSlug: ProblemSlug): string {
  const problemPath = `/problems/${problemSlugValue(problemSlug)}`
  return source.contestSlug
    ? `/contests/${contestSlugValue(source.contestSlug)}/problems/${problemSlugValue(problemSlug)}`
    : problemPath
}

export function submissionResultLabel(
  mode: SubmissionResultDisplayMode,
  status: SubmissionStatus,
  verdict: SubmissionVerdict | null,
  score: number | null,
): string {
  switch (mode) {
    case 'verdict':
      return submissionJudgeStateLabel(status, verdict)
    case 'score':
      return formatOptionalScore(score)
  }
}

export function verdictFilterLabel(verdict: SubmissionVerdictFilter, allVerdictsLabel: string): string {
  if (verdict === 'all') {
    return allVerdictsLabel
  }
  if (verdict === 'pending') {
    return submissionVerdictLabel(null)
  }
  return submissionVerdictLabel(verdict)
}
