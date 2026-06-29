import type { BorrowRecordStatus } from '@/pages/objects/BorrowRecordDisplayStatus'

export type BorrowRecordFilterStatus = 'all' | BorrowRecordStatus

export interface BorrowRecordFilter {
  readonly keyword: string
  readonly status: BorrowRecordFilterStatus
}
