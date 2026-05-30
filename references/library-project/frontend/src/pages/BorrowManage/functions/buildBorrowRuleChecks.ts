import { BorrowRecordStatuses } from '@/pages/objects/BorrowRecordDisplayStatus'
import type { BorrowRecordEntity } from '@/pages/objects/BorrowRecordEntity'

import type { BorrowDraftItem } from '../objects/BorrowDraftItem'
import type { BorrowRuleCheck } from '../objects/BorrowRuleCheck'

interface BuildBorrowRuleChecksInput {
  readonly readerName: string
  readonly draftItems: BorrowDraftItem[]
  readonly records: BorrowRecordEntity[]
  readonly borrowedCount: number
  readonly overdueCount: number
}

const maxActiveBorrowCount = 5

export function buildBorrowRuleChecks({
  readerName,
  draftItems,
  records,
  borrowedCount,
  overdueCount,
}: BuildBorrowRuleChecksInput): BorrowRuleCheck[] {
  const normalizedReaderName = readerName.trim()
  const activeRecords = records.filter((record) => record.status !== BorrowRecordStatuses.Returned)
  const draftTitles = draftItems.map((item) => item.book.title)
  const duplicatedDraftTitles = draftTitles.filter((title, index) => draftTitles.indexOf(title) !== index)
  const alreadyBorrowedTitles = draftTitles.filter((title) =>
    activeRecords.some((record) => record.readerName === normalizedReaderName && record.bookName === title),
  )
  const unavailableBooks = draftItems.filter(
    (item) => item.book.status !== 'available' || item.book.stockAvailable <= 0,
  )
  const projectedBorrowCount = borrowedCount + draftItems.length

  return [
    {
      id: 'reader-selected',
      title: '读者信息',
      description: normalizedReaderName ? `当前读者：${normalizedReaderName}` : '需要先填写借阅人姓名。',
      severity: normalizedReaderName ? 'pass' : 'block',
    },
    {
      id: 'draft-not-empty',
      title: '借阅草稿',
      description: draftItems.length > 0 ? `已选择 ${draftItems.length} 本待借图书。` : '至少选择一本图书后才能提交。',
      severity: draftItems.length > 0 ? 'pass' : 'block',
    },
    {
      id: 'reader-overdue',
      title: '逾期检查',
      description: overdueCount > 0 ? `该读者当前有 ${overdueCount} 条逾期记录。` : '未发现该读者的逾期记录。',
      severity: overdueCount > 0 ? 'block' : 'pass',
    },
    {
      id: 'borrow-limit',
      title: '借阅数量',
      description:
        projectedBorrowCount > maxActiveBorrowCount
          ? `提交后将达到 ${projectedBorrowCount} 本，超过上限 ${maxActiveBorrowCount} 本。`
          : `提交后预计共借阅 ${projectedBorrowCount} 本，未超过上限。`,
      severity: projectedBorrowCount > maxActiveBorrowCount ? 'block' : 'pass',
    },
    {
      id: 'inventory',
      title: '库存检查',
      description:
        unavailableBooks.length > 0
          ? `${unavailableBooks.map((item) => `《${item.book.title}》`).join('、')} 当前不可借。`
          : '草稿中的图书都有可借库存。',
      severity: unavailableBooks.length > 0 ? 'block' : 'pass',
    },
    {
      id: 'duplicate-books',
      title: '重复借阅',
      description:
        duplicatedDraftTitles.length > 0 || alreadyBorrowedTitles.length > 0
          ? '草稿中存在重复图书，或读者已经借阅同名图书。'
          : '未发现重复借阅风险。',
      severity: duplicatedDraftTitles.length > 0 || alreadyBorrowedTitles.length > 0 ? 'warning' : 'pass',
    },
  ]
}
