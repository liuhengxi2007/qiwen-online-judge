import type { BookRecord } from '@/objects/books/BookRecord'

import type { BorrowDraftItem } from './BorrowDraftItem'

export interface BorrowDraft {
  readonly items: BorrowDraftItem[]
  readonly itemCount: number
  readonly addBook: (book: BookRecord) => void
  readonly removeBook: (bookId: string) => void
  readonly updateDueDate: (bookId: string, dueDate: string) => void
  readonly clear: () => void
  readonly hasBook: (bookId: string) => boolean
}
