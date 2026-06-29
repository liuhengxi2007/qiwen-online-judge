import { RefreshCw, Search } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

import type { BorrowRecordFilterStatus } from '../objects/BorrowRecordFilter'

export function BorrowRecordToolbar({
  keyword,
  status,
  statusOptions,
  loading,
  onKeywordChange,
  onStatusChange,
  onRefresh,
}: {
  keyword: string
  status: BorrowRecordFilterStatus
  statusOptions: { value: BorrowRecordFilterStatus; label: string }[]
  loading: boolean
  onKeywordChange: (value: string) => void
  onStatusChange: (value: BorrowRecordFilterStatus) => void
  onRefresh: () => void
}) {
  return (
    <div className="grid gap-3 lg:grid-cols-[1fr_160px_auto]">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
        <Input
          value={keyword}
          className="h-10 rounded-lg border-slate-200 pl-9"
          placeholder="搜索借阅编号、图书或读者"
          onChange={(event) => onKeywordChange(event.target.value)}
        />
      </div>

      <Select value={status} onValueChange={(value) => onStatusChange(value as BorrowRecordFilterStatus)}>
        <SelectTrigger className="h-10 rounded-lg border-slate-200">
          <SelectValue placeholder="全部状态" />
        </SelectTrigger>
        <SelectContent>
          {statusOptions.map((option) => (
            <SelectItem key={option.value} value={option.value}>
              {option.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Button
        type="button"
        variant="outline"
        className="h-10 rounded-lg border-slate-200"
        disabled={loading}
        onClick={onRefresh}
      >
        <RefreshCw className="size-4" />
        刷新记录
      </Button>
    </div>
  )
}
