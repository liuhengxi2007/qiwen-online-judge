import { useMemo, useState } from 'react'

import { BorrowRecordStatuses } from '@/pages/objects/BorrowRecordDisplayStatus'
import type { BorrowRecordEntity } from '@/pages/objects/BorrowRecordEntity'

import { filterBorrowRecords } from '../functions/filterBorrowRecords'
import type { BorrowRecordFilter, BorrowRecordFilterStatus } from '../objects/BorrowRecordFilter'

export function useBorrowRecordPanel(records: BorrowRecordEntity[]) {
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<BorrowRecordFilterStatus>('all')

  const filter = useMemo<BorrowRecordFilter>(
    () => ({
      keyword,
      status,
    }),
    [keyword, status],
  )

  const visibleRecords = useMemo(() => filterBorrowRecords(records, filter), [filter, records])

  return {
    filter,
    visibleRecords,
    statusOptions: [
      { value: 'all', label: '全部状态' },
      { value: BorrowRecordStatuses.Borrowing, label: BorrowRecordStatuses.Borrowing },
      { value: BorrowRecordStatuses.Overdue, label: BorrowRecordStatuses.Overdue },
      { value: BorrowRecordStatuses.Returned, label: BorrowRecordStatuses.Returned },
    ] satisfies { value: BorrowRecordFilterStatus; label: string }[],
    setKeyword,
    setStatus,
  }
}
