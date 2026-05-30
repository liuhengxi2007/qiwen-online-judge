import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'

import { BorrowBookAPI } from '@/apis/books/BorrowBookAPI'
import { ListBooksAPI } from '@/apis/books/ListBooksAPI'
import type { BookRecord } from '@/objects/books/BookRecord'
import { BorrowRecordStatuses } from '@/pages/objects/BorrowRecordDisplayStatus'
import { sendAPI } from '@/system/api/sendAPI'

import { buildBorrowRuleChecks } from '../functions/buildBorrowRuleChecks'
import { buildBorrowSummary } from '../functions/buildBorrowSummary'
import type { BorrowManagePageState } from '../objects/BorrowManagePageState'
import { useBorrowDraft } from './useBorrowDraft'
import { useBorrowRecords } from './useBorrowRecords'

export function useBorrowManagePage(): BorrowManagePageState {
  const [searchParams] = useSearchParams()
  const draft = useBorrowDraft()
  const records = useBorrowRecords()

  const [readerName, setReaderName] = useState('代办读者')
  const [books, setBooks] = useState<BookRecord[]>([])
  const [bookKeyword, setBookKeyword] = useState(searchParams.get('bookName') ?? '')
  const [bookCategory, setBookCategory] = useState('all')
  const [availableOnly, setAvailableOnly] = useState(true)
  const [booksLoading, setBooksLoading] = useState(true)
  const [booksErrorMessage, setBooksErrorMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submitMessage, setSubmitMessage] = useState('')

  const refreshBooks = useCallback(async () => {
    setBooksLoading(true)

    try {
      const response = await sendAPI(new ListBooksAPI(bookKeyword))
      setBooks(response.books)
      setBooksErrorMessage('')
    } catch (error) {
      setBooksErrorMessage(error instanceof Error ? error.message : '图书列表加载失败，请稍后重试。')
    } finally {
      setBooksLoading(false)
    }
  }, [bookKeyword])

  useEffect(() => {
    void refreshBooks()
  }, [refreshBooks])

  useEffect(() => {
    const pendingBookId = searchParams.get('bookId')
    if (!pendingBookId || draft.hasBook(pendingBookId)) {
      return
    }

    const pendingBook = books.find((book) => book.id === pendingBookId)
    if (pendingBook) {
      draft.addBook(pendingBook)
    }
  }, [books, draft, searchParams])

  const categories = useMemo(
    () => Array.from(new Set(books.map((book) => book.categoryLabel))).sort(),
    [books],
  )

  const readerRecords = useMemo(() => {
    const normalizedReaderName = readerName.trim()
    if (!normalizedReaderName) {
      return []
    }

    return records.records.filter((record) => record.readerName === normalizedReaderName)
  }, [readerName, records.records])

  const borrowedCount = useMemo(
    () => readerRecords.filter((record) => record.status !== BorrowRecordStatuses.Returned).length,
    [readerRecords],
  )

  const overdueCount = useMemo(
    () => readerRecords.filter((record) => record.status === BorrowRecordStatuses.Overdue).length,
    [readerRecords],
  )

  const returnedCount = useMemo(
    () => readerRecords.filter((record) => record.status === BorrowRecordStatuses.Returned).length,
    [readerRecords],
  )

  const ruleChecks = useMemo(
    () =>
      buildBorrowRuleChecks({
        readerName,
        draftItems: draft.items,
        records: records.records,
        borrowedCount,
        overdueCount,
      }),
    [borrowedCount, draft.items, overdueCount, readerName, records.records],
  )

  const summary = useMemo(
    () =>
      buildBorrowSummary({
        readerName,
        draftCount: draft.itemCount,
        activeBorrowCount: records.activeCount,
        ruleChecks,
      }),
    [draft.itemCount, readerName, records.activeCount, ruleChecks],
  )

  const canSubmit = ruleChecks.every((check) => check.severity !== 'block') && draft.itemCount > 0 && !submitting

  const submit = useCallback(async () => {
    if (!canSubmit) {
      setSubmitMessage('当前借阅草稿还没有通过规则检查，暂时不能提交。')
      return
    }

    setSubmitting(true)
    setSubmitMessage('')

    try {
      const createdRecords = []
      const normalizedReaderName = readerName.trim()

      for (const item of draft.items) {
        const record = await sendAPI(new BorrowBookAPI(item.book.id, normalizedReaderName))
        createdRecords.push(record)
      }

      records.prependRecords(createdRecords)
      draft.clear()
      setSubmitMessage(`已完成 ${createdRecords.length} 本图书的借书登记。`)
    } catch (error) {
      setSubmitMessage(error instanceof Error ? error.message : '借书登记失败，请稍后重试。')
    } finally {
      setSubmitting(false)
    }
  }, [canSubmit, draft, readerName, records])

  return {
    summary,
    reader: {
      readerName,
      borrowedCount,
      overdueCount,
      returnedCount,
      canBorrow: readerName.trim().length > 0 && overdueCount === 0,
      setReaderName,
    },
    books: {
      books,
      categories,
      keyword: bookKeyword,
      category: bookCategory,
      availableOnly,
      loading: booksLoading,
      errorMessage: booksErrorMessage,
      setKeyword: setBookKeyword,
      setCategory: setBookCategory,
      setAvailableOnly,
      refresh: refreshBooks,
    },
    draft,
    ruleChecks,
    confirmation: {
      canSubmit,
      submitting,
      message: submitMessage,
      submit,
    },
    records: {
      records: records.records,
      activeCount: records.activeCount,
      loading: records.loading,
      message: records.message,
      refresh: records.refresh,
      returnBook: records.returnBook,
      goToRecord: records.goToRecord,
    },
  }
}
