import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

import type { BorrowRecordsState } from '../../objects/BorrowManagePageState'
import { BorrowRecordTable } from './components/BorrowRecordTable'
import { BorrowRecordToolbar } from './components/BorrowRecordToolbar'
import { useBorrowRecordPanel } from './hooks/useBorrowRecordPanel'

export function BorrowRecordPanel({ records }: { records: BorrowRecordsState }) {
  const panel = useBorrowRecordPanel(records.records)

  return (
    <Card className="gap-0 rounded-lg border-slate-200 py-0 shadow-sm">
      <CardHeader className="border-b border-slate-200 px-5 py-4">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <CardTitle className="text-base text-slate-950">当前借阅记录</CardTitle>
            <CardDescription>记录区继续拆出工具栏、表格、行、状态徽标和操作按钮。</CardDescription>
          </div>
          <div className="text-sm text-slate-500">活跃记录 {records.activeCount} 条</div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4 px-5 py-4">
        <BorrowRecordToolbar
          keyword={panel.filter.keyword}
          status={panel.filter.status}
          statusOptions={panel.statusOptions}
          loading={records.loading}
          onKeywordChange={panel.setKeyword}
          onStatusChange={panel.setStatus}
          onRefresh={() => void records.refresh()}
        />

        {records.message ? (
          <Alert className="rounded-lg border-amber-200 bg-amber-50">
            <AlertDescription className="text-sm text-amber-700">{records.message}</AlertDescription>
          </Alert>
        ) : null}

        <BorrowRecordTable
          records={panel.visibleRecords}
          loading={records.loading}
          onReturnBook={(recordId) => void records.returnBook(recordId)}
          onOpenDetail={records.goToRecord}
        />
      </CardContent>
    </Card>
  )
}
