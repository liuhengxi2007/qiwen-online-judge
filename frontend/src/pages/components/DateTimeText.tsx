import { formatDateTime, formatUtcOffsetTitle } from '@/system/format/date-time'

/**
 * 时间文本组件属性，value 为后端时间字符串，className 用于外部布局。
 */
type DateTimeTextProps = {
  value: string
  className?: string
}

/**
 * 渲染语义化 time 元素；正文展示本地时间，title 展示 UTC 偏移提示。
 */
export function DateTimeText({ value, className }: DateTimeTextProps) {
  const title = formatUtcOffsetTitle(value)

  return (
    <time className={className} dateTime={value} title={title || undefined}>
      {formatDateTime(value)}
    </time>
  )
}
