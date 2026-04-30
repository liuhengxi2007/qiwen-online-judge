import { formatDateTime, formatUtcOffsetTitle } from '@/shared/lib/date-time'

export const formatBlogDate = formatDateTime

export const formatBlogDateTitle = formatUtcOffsetTitle

export function blogScoreClassName(score: number): string {
  if (score > 0) {
    return 'text-emerald-700'
  }

  if (score < 0) {
    return 'text-rose-700'
  }

  return 'text-slate-700'
}
