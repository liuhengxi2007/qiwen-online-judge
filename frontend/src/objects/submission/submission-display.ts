import { formatDateTime, formatUtcOffsetTitle } from '@/objects/shared/date-time'
import type { SubmissionVerdictFilter } from '@/objects/submission/request/SubmissionVerdictFilter'
import { submissionVerdictLabel } from '@/objects/submission/submission-parsers'

export function formatOptionalDurationMs(value: number | null): string {
  if (value === null) {
    return '--'
  }

  return `${value} ms`
}

export function formatOptionalMemoryKb(value: number | null): string {
  if (value === null) {
    return '--'
  }

  const kib = value
  const mib = value / 1024
  const gib = value / (1024 * 1024)

  if (kib < 1000) {
    return `${formatWithReadablePrecision(kib)} KiB`
  }

  if (mib < 1000) {
    return `${formatWithReadablePrecision(mib)} MiB`
  }

  return `${formatWithReadablePrecision(gib)} GiB`
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

function formatWithReadablePrecision(value: number): string {
  if (value < 1) {
    return new Intl.NumberFormat(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value)
  }

  return new Intl.NumberFormat(undefined, {
    maximumSignificantDigits: 3,
  }).format(value)
}
