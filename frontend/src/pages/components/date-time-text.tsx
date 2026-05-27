import { formatDateTime, formatUtcOffsetTitle } from '@/objects/shared/date-time'

type DateTimeTextProps = {
  value: string
  className?: string
}

export function DateTimeText({ value, className }: DateTimeTextProps) {
  const title = formatUtcOffsetTitle(value)

  return (
    <time className={className} dateTime={value} title={title || undefined}>
      {formatDateTime(value)}
    </time>
  )
}
