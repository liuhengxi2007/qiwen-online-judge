import { Badge } from '@/components/ui/badge'
import { cn } from '@/components/ui/utils'
import { BorrowRecordStatuses } from '@/pages/objects/BorrowRecordDisplayStatus'
import type { BorrowRecordStatus } from '@/pages/objects/BorrowRecordDisplayStatus'

export function BorrowRecordStatusBadge({ status }: { status: BorrowRecordStatus }) {
  return (
    <Badge
      variant="outline"
      className={cn(
        'rounded-full border px-3 py-1 text-xs font-medium',
        status === BorrowRecordStatuses.Borrowing && 'border-sky-200 bg-sky-50 text-sky-700',
        status === BorrowRecordStatuses.Overdue && 'border-amber-200 bg-amber-50 text-amber-700',
        status === BorrowRecordStatuses.Returned && 'border-emerald-200 bg-emerald-50 text-emerald-700',
      )}
    >
      {status}
    </Badge>
  )
}
