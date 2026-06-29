import {
  TableCell,
  TableRow,
} from '@/components/ui/table'
import type { BorrowRecordEntity } from '@/pages/objects/BorrowRecordEntity'

import { BorrowRecordActions } from './BorrowRecordActions'
import { BorrowRecordStatusBadge } from './BorrowRecordStatusBadge'

export function BorrowRecordRow({
  record,
  onReturn,
  onOpenDetail,
}: {
  record: BorrowRecordEntity
  onReturn: () => void
  onOpenDetail: () => void
}) {
  return (
    <TableRow className="border-slate-200/80">
      <TableCell className="pl-5 align-middle text-sm font-medium text-slate-700">{record.id}</TableCell>
      <TableCell className="align-middle text-sm text-slate-700">
        <div className="space-y-1">
          <div>{record.bookName}</div>
          {record.returnDate ? <div className="text-xs text-slate-400">归还日期 {record.returnDate}</div> : null}
        </div>
      </TableCell>
      <TableCell className="align-middle text-sm text-slate-700">{record.readerName}</TableCell>
      <TableCell className="align-middle text-sm text-slate-600">{record.borrowDate}</TableCell>
      <TableCell className="align-middle text-sm text-slate-600">{record.dueDate}</TableCell>
      <TableCell className="align-middle">
        <BorrowRecordStatusBadge status={record.status} />
      </TableCell>
      <TableCell className="pr-5 text-right align-middle">
        <BorrowRecordActions record={record} onReturn={onReturn} onOpenDetail={onOpenDetail} />
      </TableCell>
    </TableRow>
  )
}
