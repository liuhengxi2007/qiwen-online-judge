import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { ListBorrowRecordsAPI } from '@/apis/books/ListBorrowRecordsAPI'
import { ReturnBorrowRecordAPI } from '@/apis/books/ReturnBorrowRecordAPI'
import type { BorrowRecord } from '@/objects/books/BorrowRecord'
import { BorrowRecordStatuses } from '@/pages/objects/BorrowRecordDisplayStatus'
import { toBorrowRecordEntity } from '@/pages/objects/mappers'
import { sendAPI } from '@/system/api/sendAPI'

export function useBorrowRecords() {
  const navigate = useNavigate()
  const [records, setRecords] = useState(() => [] as ReturnType<typeof toBorrowRecordEntity>[])
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')

  const refresh = useCallback(async () => {
    setLoading(true)

    try {
      const response = await sendAPI(new ListBorrowRecordsAPI())
      setRecords(response.records.map(toBorrowRecordEntity))
      setMessage('')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '借阅记录加载失败，请稍后重试。')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  const prependRecords = useCallback((createdRecords: BorrowRecord[]) => {
    setRecords((current) => [...createdRecords.map(toBorrowRecordEntity), ...current])
  }, [])

  const returnBook = useCallback(async (recordId: string) => {
    try {
      const response = await sendAPI(new ReturnBorrowRecordAPI(recordId))
      setRecords((current) =>
        current.map((item) => (item.id === recordId ? toBorrowRecordEntity(response.record) : item)),
      )
      setMessage('归还办理完成。')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '归还办理失败，请稍后重试。')
    }
  }, [])

  const activeCount = useMemo(
    () => records.filter((record) => record.status !== BorrowRecordStatuses.Returned).length,
    [records],
  )

  return {
    records,
    activeCount,
    loading,
    message,
    refresh,
    returnBook,
    prependRecords,
    goToRecord: (recordId: string) => navigate(`/library/admin/borrow/record/${recordId}`),
  }
}
