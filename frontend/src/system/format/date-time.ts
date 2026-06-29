/**
 * 将日期时间片段补齐为两位字符串，用于本地时间格式化输出。
 */
function padTwoDigits(value: number): string {
  return String(value).padStart(2, '0')
}

/**
 * 安全解析日期字符串；无效输入返回 null，让调用方决定展示原值还是空标题。
 */
function parseDate(value: string): Date | null {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

/**
 * 将可解析时间字符串格式化为本地 yyyy-MM-dd HH:mm:ss；无效输入保持原文以免隐藏后端数据。
 */
export function formatDateTime(value: string): string {
  const date = parseDate(value)
  if (!date) {
    return value
  }

  return `${date.getFullYear()}-${padTwoDigits(date.getMonth() + 1)}-${padTwoDigits(date.getDate())} ${padTwoDigits(date.getHours())}:${padTwoDigits(date.getMinutes())}:${padTwoDigits(date.getSeconds())}`
}

/**
 * 根据传入时间在当前环境中的时区偏移生成 UTC 标题；无效输入返回空字符串。
 */
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
