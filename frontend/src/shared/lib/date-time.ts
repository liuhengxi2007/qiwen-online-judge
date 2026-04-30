function padTwoDigits(value: number): string {
  return String(value).padStart(2, '0')
}

function parseDate(value: string): Date | null {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

export function formatDateTime(value: string): string {
  const date = parseDate(value)
  if (!date) {
    return value
  }

  return `${date.getFullYear()}-${padTwoDigits(date.getMonth() + 1)}-${padTwoDigits(date.getDate())} ${padTwoDigits(date.getHours())}:${padTwoDigits(date.getMinutes())}:${padTwoDigits(date.getSeconds())}`
}

export function formatUtcOffsetTitle(value: string): string {
  const date = parseDate(value)
  if (!date) {
    return ''
  }

  const totalOffsetMinutes = -date.getTimezoneOffset()
  const sign = totalOffsetMinutes >= 0 ? '+' : '-'
  const absoluteOffsetMinutes = Math.abs(totalOffsetMinutes)
  const offsetHours = Math.floor(absoluteOffsetMinutes / 60)
  const offsetMinutes = absoluteOffsetMinutes % 60

  return `UTC${sign}${padTwoDigits(offsetHours)}:${padTwoDigits(offsetMinutes)}`
}
