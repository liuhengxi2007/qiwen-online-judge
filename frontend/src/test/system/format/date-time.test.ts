import { describe, expect, it } from 'vitest'

import { formatDateTime, formatUtcOffsetTitle } from '@/system/format/date-time'

function expectedUtcOffset(date: Date): string {
  const totalOffsetMinutes = -date.getTimezoneOffset()
  const sign = totalOffsetMinutes >= 0 ? '+' : '-'
  const absoluteOffsetMinutes = Math.abs(totalOffsetMinutes)
  const offsetHours = Math.floor(absoluteOffsetMinutes / 60)
  const offsetMinutes = absoluteOffsetMinutes % 60

  return `UTC${sign}${String(offsetHours).padStart(2, '0')}:${String(offsetMinutes).padStart(2, '0')}`
}

describe('date-time formatting', () => {
  it('formats local date-time with zero-padded fields', () => {
    const localDate = new Date(2026, 0, 2, 3, 4, 5)

    expect(formatDateTime(localDate.toISOString())).toBe('2026-01-02 03:04:05')
  })

  it('formats UTC offset titles from the local offset at the target instant', () => {
    const localDate = new Date(2026, 3, 29, 12, 0, 0)

    expect(formatUtcOffsetTitle(localDate.toISOString())).toBe(expectedUtcOffset(localDate))
  })

  it('formats offset-aware timestamps using local display time', () => {
    const localDate = new Date(2026, 6, 1, 8, 9, 10)
    const offsetAwareValue = localDate.toISOString().replace('Z', '+00:00')

    expect(formatDateTime(offsetAwareValue)).toBe('2026-07-01 08:09:10')
    expect(formatUtcOffsetTitle(offsetAwareValue)).toBe(expectedUtcOffset(localDate))
  })

  it('returns fallback values for invalid timestamps', () => {
    expect(formatDateTime('not-a-timestamp')).toBe('not-a-timestamp')
    expect(formatUtcOffsetTitle('not-a-timestamp')).toBe('')
  })
})
