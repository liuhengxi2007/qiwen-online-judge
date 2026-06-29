import { FileSearch, Undo2 } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { BorrowRecordStatuses } from '@/pages/objects/BorrowRecordDisplayStatus'
import type { BorrowRecordEntity } from '@/pages/objects/BorrowRecordEntity'

export function BorrowRecordActions({
  record,
  onReturn,
  onOpenDetail,
}: {
  record: BorrowRecordEntity
  onReturn: () => void
  onOpenDetail: () => void
}) {
  return (
    <div className="flex justify-end gap-2">
      <Button type="button" variant="ghost" className="h-9 rounded-lg px-3 text-slate-600" onClick={onOpenDetail}>
        <FileSearch className="size-4" />
        详情
      </Button>
      {record.status !== BorrowRecordStatuses.Returned ? (
        <Button
          type="button"
          variant="outline"
          className="h-9 rounded-lg border-slate-200 px-3 text-slate-700"
          onClick={onReturn}
        >
          <Undo2 className="size-4" />
          归还
        </Button>
      ) : null}
    </div>
  )
}
