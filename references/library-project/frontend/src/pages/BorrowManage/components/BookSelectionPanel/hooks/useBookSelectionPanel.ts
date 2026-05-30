import { useMemo } from 'react'

import type { BorrowDraft } from '../../../objects/BorrowDraft'
import type { BookSelectionState } from '../../../objects/BorrowManagePageState'
import type { BookCandidateView } from '../objects/BookCandidateView'

export function useBookSelectionPanel(books: BookSelectionState, draft: BorrowDraft) {
  const candidates = useMemo<BookCandidateView[]>(
    () =>
      books.books
        .filter((book) => books.category === 'all' || book.categoryLabel === books.category)
        .filter((book) => !books.availableOnly || (book.status === 'available' && book.stockAvailable > 0))
        .map((book) => ({
          book,
          canBorrow: book.status === 'available' && book.stockAvailable > 0,
          isInDraft: draft.hasBook(book.id),
          inventoryLabel: `${book.stockAvailable}/${book.stockTotal} 可借`,
        })),
    [books.availableOnly, books.books, books.category, draft],
  )

  return {
    candidates,
    selectedCount: draft.itemCount,
    availableCandidateCount: candidates.filter((candidate) => candidate.canBorrow).length,
  }
}
