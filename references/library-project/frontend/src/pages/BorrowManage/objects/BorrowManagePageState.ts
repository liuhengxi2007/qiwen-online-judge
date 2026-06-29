import type { BookRecord } from '@/objects/books/BookRecord'
import type { BorrowRecordEntity } from '@/pages/objects/BorrowRecordEntity'

import type { BorrowDraft } from './BorrowDraft'
import type { BorrowManageSummary } from './BorrowManageSummary'
import type { BorrowRuleCheck } from './BorrowRuleCheck'

export interface ReaderPanelState {
  readonly readerName: string
  readonly borrowedCount: number
  readonly overdueCount: number
  readonly returnedCount: number
  readonly canBorrow: boolean
  readonly setReaderName: (value: string) => void
}

export interface BookSelectionState {
  readonly books: BookRecord[]
  readonly categories: string[]
  readonly keyword: string
  readonly category: string
  readonly availableOnly: boolean
  readonly loading: boolean
  readonly errorMessage: string
  readonly setKeyword: (value: string) => void
  readonly setCategory: (value: string) => void
  readonly setAvailableOnly: (value: boolean) => void
  readonly refresh: () => Promise<void>
}

export interface BorrowConfirmationState {
  readonly canSubmit: boolean
  readonly submitting: boolean
  readonly message: string
  readonly submit: () => Promise<void>
}

export interface BorrowRecordsState {
  readonly records: BorrowRecordEntity[]
  readonly activeCount: number
  readonly loading: boolean
  readonly message: string
  readonly refresh: () => Promise<void>
  readonly returnBook: (recordId: string) => Promise<void>
  readonly goToRecord: (recordId: string) => void
}

export interface BorrowManagePageState {
  readonly summary: BorrowManageSummary
  readonly reader: ReaderPanelState
  readonly books: BookSelectionState
  readonly draft: BorrowDraft
  readonly ruleChecks: BorrowRuleCheck[]
  readonly confirmation: BorrowConfirmationState
  readonly records: BorrowRecordsState
}
