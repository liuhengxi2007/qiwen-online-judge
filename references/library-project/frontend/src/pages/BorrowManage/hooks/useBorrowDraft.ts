import { useCallback, useMemo, useState } from 'react'

import type { BookRecord } from '@/objects/books/BookRecord'

import type { BorrowDraft } from '../objects/BorrowDraft'
import type { BorrowDraftItem } from '../objects/BorrowDraftItem'

function createDefaultDueDate() {
  const dueDate = new Date()
  dueDate.setDate(dueDate.getDate() + 30)
  return dueDate.toISOString().slice(0, 10)
}

export function useBorrowDraft(): BorrowDraft {
  const [items, setItems] = useState<BorrowDraftItem[]>([])

  const addBook = useCallback((book: BookRecord) => {
    setItems((current) => {
      if (current.some((item) => item.book.id === book.id)) {
        return current
      }

      return [...current, { book, dueDate: createDefaultDueDate() }]
    })
  }, [])

  const removeBook = useCallback((bookId: string) => {
    setItems((current) => current.filter((item) => item.book.id !== bookId))
  }, [])

  const updateDueDate = useCallback((bookId: string, dueDate: string) => {
    setItems((current) =>
      current.map((item) => (item.book.id === bookId ? { ...item, dueDate } : item)),
    )
  }, [])

  const clear = useCallback(() => {
    setItems([])
  }, [])

  const hasBook = useCallback(
    (bookId: string) => items.some((item) => item.book.id === bookId),
    [items],
  )

  return useMemo(
    () => ({
      items,
      itemCount: items.length,
      addBook,
      removeBook,
      updateDueDate,
      clear,
      hasBook,
    }),
    [addBook, clear, hasBook, items, removeBook, updateDueDate],
  )
}
