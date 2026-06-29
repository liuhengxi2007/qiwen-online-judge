import type { BookRecord } from '@/objects/books/BookRecord'

export interface BorrowDraftItem {
  readonly book: BookRecord
  readonly dueDate: string
}
