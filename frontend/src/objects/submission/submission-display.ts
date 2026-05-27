import { formatDateTime, formatUtcOffsetTitle } from '@/objects/shared/date-time'
import { formatOptionalBinarySizeBytes } from '@/objects/shared/format/binary-size'
import type { SubmissionVerdictFilter } from '@/objects/submission/request/SubmissionVerdictFilter'
import { submissionVerdictLabel } from '@/objects/submission/submission-parsers'

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

export function verdictFilterLabel(verdict: SubmissionVerdictFilter, allVerdictsLabel: string): string {
  if (verdict === 'all') {
    return allVerdictsLabel
  }
  if (verdict === 'pending') {
    return submissionVerdictLabel(null)
  }
  return submissionVerdictLabel(verdict)
}
