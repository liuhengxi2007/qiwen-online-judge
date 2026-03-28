export const BorrowRecordStatuses = {
  Borrowing: '借阅中',
  Overdue: '已逾期',
  Returned: '已归还',
} as const

export type BorrowRecordStatus =
  (typeof BorrowRecordStatuses)[keyof typeof BorrowRecordStatuses]

export const ReturnRecordStatuses = {
  Pending: '待归还',
  Overdue: '已逾期',
  Returned: '已归还',
} as const

export type ReturnRecordStatus =
  (typeof ReturnRecordStatuses)[keyof typeof ReturnRecordStatuses]

export const CirculationReviewStatuses = {
  None: 'none',
  ManualReview: 'manual_review',
  FineRequired: 'fine_required',
  RetryRequired: 'retry_required',
} as const

export type CirculationReviewStatus =
  (typeof CirculationReviewStatuses)[keyof typeof CirculationReviewStatuses]

export class BorrowRecordEntity {
  readonly id: string
  readonly bookName: string
  readonly readerName: string
  readonly borrowDate: string
  readonly dueDate: string
  readonly status: BorrowRecordStatus
  readonly returnDate?: string

  constructor(
    id: string,
    bookName: string,
    readerName: string,
    borrowDate: string,
    dueDate: string,
    status: BorrowRecordStatus,
    returnDate?: string,
  ) {
    this.id = id
    this.bookName = bookName
    this.readerName = readerName
    this.borrowDate = borrowDate
    this.dueDate = dueDate
    this.status = status
    this.returnDate = returnDate
  }
}

export class ReturnRecordEntity {
  readonly id: string
  readonly bookTitle: string
  readonly readerName: string
  readonly borrowCode: string
  readonly borrowDate: string
  readonly dueDate: string
  readonly status: ReturnRecordStatus
  readonly returnedDate?: string

  constructor(
    id: string,
    bookTitle: string,
    readerName: string,
    borrowCode: string,
    borrowDate: string,
    dueDate: string,
    status: ReturnRecordStatus,
    returnedDate?: string,
  ) {
    this.id = id
    this.bookTitle = bookTitle
    this.readerName = readerName
    this.borrowCode = borrowCode
    this.borrowDate = borrowDate
    this.dueDate = dueDate
    this.status = status
    this.returnedDate = returnedDate
  }
}

export interface BorrowCreationRequest {
  bookId?: string
  bookName?: string
}

export type BorrowCreationResponse =
  | {
      ok: true
      record: BorrowRecordEntity
    }
  | {
      ok: false
      reason: 'book_unavailable' | 'reader_restricted'
      message: string
    }

export interface ReturnProcessRequest {
  recordId: string
}

export type ReturnProcessSuccessResponse = {
  ok: true
  reviewStatus: typeof CirculationReviewStatuses.None
  recordId: string
  returnedDate: string
}

export type ReturnProcessFailureResponse = {
  ok: false
  reviewStatus:
    | typeof CirculationReviewStatuses.ManualReview
    | typeof CirculationReviewStatuses.FineRequired
    | typeof CirculationReviewStatuses.RetryRequired
  message: string
  recordId: string
}

export type ReturnProcessResponse = ReturnProcessSuccessResponse | ReturnProcessFailureResponse
