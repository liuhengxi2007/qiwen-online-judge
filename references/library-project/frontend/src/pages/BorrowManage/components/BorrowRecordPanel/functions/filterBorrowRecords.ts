import type { BorrowRecordEntity } from '@/pages/objects/BorrowRecordEntity'

import type { BorrowRecordFilter } from '../objects/BorrowRecordFilter'

export function filterBorrowRecords(records: BorrowRecordEntity[], filter: BorrowRecordFilter) {
  const keyword = filter.keyword.trim().toLowerCase()

  return records.filter((record) => {
    const matchesKeyword =
      !keyword ||
      record.id.toLowerCase().includes(keyword) ||
      record.bookName.toLowerCase().includes(keyword) ||
      record.readerName.toLowerCase().includes(keyword)
    const matchesStatus = filter.status === 'all' || record.status === filter.status

    return matchesKeyword && matchesStatus
  })
}
