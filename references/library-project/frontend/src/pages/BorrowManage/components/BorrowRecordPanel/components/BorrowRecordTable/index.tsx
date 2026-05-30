import {
  Table,
  TableBody,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import type { BorrowRecordEntity } from '@/pages/objects/BorrowRecordEntity'

import { BorrowRecordRow } from './components/BorrowRecordRow'

export function BorrowRecordTable({
  records,
  loading,
  onReturnBook,
  onOpenDetail,
}: {
  records: BorrowRecordEntity[]
  loading: boolean
  onReturnBook: (recordId: string) => void
  onOpenDetail: (recordId: string) => void
}) {
  if (loading) {
    return <div className="rounded-lg border border-slate-200 bg-slate-50 p-5 text-sm text-slate-500">正在加载借阅记录...</div>
  }

  if (records.length === 0) {
    return <div className="rounded-lg border border-slate-200 bg-slate-50 p-5 text-sm text-slate-500">没有符合条件的借阅记录。</div>
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
      <Table>
        <TableHeader>
          <TableRow className="bg-slate-50/80 hover:bg-slate-50/80">
            <TableHead className="h-12 pl-5 text-slate-500">借阅编号</TableHead>
            <TableHead className="text-slate-500">图书</TableHead>
            <TableHead className="text-slate-500">读者</TableHead>
            <TableHead className="text-slate-500">借出日期</TableHead>
            <TableHead className="text-slate-500">应还日期</TableHead>
            <TableHead className="text-slate-500">状态</TableHead>
            <TableHead className="pr-5 text-right text-slate-500">操作</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {records.map((record) => (
            <BorrowRecordRow
              key={record.id}
              record={record}
              onReturn={() => onReturnBook(record.id)}
              onOpenDetail={() => onOpenDetail(record.id)}
            />
          ))}
        </TableBody>
      </Table>
    </div>
  )
}
