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

function localDate(input: {
  year: number
  monthIndex: number
  day: number
  hours: number
  minutes: number
  seconds: number
}): Date {
  // 保留 Date 构造器位置参数：这是浏览器原生 API，外层 helper 已经把调用端改为具名字段。
  return new Date(input.year, input.monthIndex, input.day, input.hours, input.minutes, input.seconds)
}

describe('date-time formatting', () => {
  it('formats local date-time with zero-padded fields', () => {
    const localDateValue = localDate({ year: 2026, monthIndex: 0, day: 2, hours: 3, minutes: 4, seconds: 5 })

    expect(formatDateTime(localDateValue.toISOString())).toBe('2026-01-02 03:04:05')
  })

  it('formats UTC offset titles from the local offset at the target instant', () => {
    const localDateValue = localDate({ year: 2026, monthIndex: 3, day: 29, hours: 12, minutes: 0, seconds: 0 })

    expect(formatUtcOffsetTitle(localDateValue.toISOString())).toBe(expectedUtcOffset(localDateValue))
  })

  it('formats offset-aware timestamps using local display time', () => {
    const localDateValue = localDate({ year: 2026, monthIndex: 6, day: 1, hours: 8, minutes: 9, seconds: 10 })
    const offsetAwareValue = localDateValue.toISOString().replace('Z', '+00:00')

    expect(formatDateTime(offsetAwareValue)).toBe('2026-07-01 08:09:10')
    expect(formatUtcOffsetTitle(offsetAwareValue)).toBe(expectedUtcOffset(localDateValue))
  })

  it('returns fallback values for invalid timestamps', () => {
    expect(formatDateTime('not-a-timestamp')).toBe('not-a-timestamp')
    expect(formatUtcOffsetTitle('not-a-timestamp')).toBe('')
  })
})
