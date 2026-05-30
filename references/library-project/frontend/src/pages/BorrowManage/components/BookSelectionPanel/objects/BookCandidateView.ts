import type { BookRecord } from '@/objects/books/BookRecord'

export interface BookCandidateView {
  readonly book: BookRecord
  readonly canBorrow: boolean
  readonly isInDraft: boolean
  readonly inventoryLabel: string
}
