export const BookCategoryCodes = {
  Computer: 'computer',
  Literature: 'literature',
  History: 'history',
  Management: 'management',
  SciFi: 'scifi',
  Novel: 'novel',
} as const

export type BookCategoryCode = (typeof BookCategoryCodes)[keyof typeof BookCategoryCodes]

export const BookInventoryStatuses = {
  Available: 'available',
  Borrowed: 'borrowed',
} as const

export type BookInventoryStatus =
  (typeof BookInventoryStatuses)[keyof typeof BookInventoryStatuses]

export class BookCatalogRecord {
  readonly id: string
  readonly title: string
  readonly author: string
  readonly categoryLabel: string
  readonly isbn: string
  readonly status: BookInventoryStatus

  constructor(
    id: string,
    title: string,
    author: string,
    categoryLabel: string,
    isbn: string,
    status: BookInventoryStatus,
  ) {
    this.id = id
    this.title = title
    this.author = author
    this.categoryLabel = categoryLabel
    this.isbn = isbn
    this.status = status
  }
}

export class BookFormDraft {
  readonly title: string
  readonly author: string
  readonly isbn: string
  readonly category: BookCategoryCode | ''
  readonly stockText: string
  readonly summary: string

  constructor(
    title: string,
    author: string,
    isbn: string,
    category: BookCategoryCode | '',
    stockText: string,
    summary: string,
  ) {
    this.title = title
    this.author = author
    this.isbn = isbn
    this.category = category
    this.stockText = stockText
    this.summary = summary
  }

  static fromForm(
    title: string,
    author: string,
    isbn: string,
    category: BookCategoryCode | '',
    stockText: string,
    summary: string,
  ) {
    return new BookFormDraft(
      title.trim(),
      author.trim(),
      isbn.trim(),
      category,
      stockText.trim(),
      summary.trim(),
    )
  }

  get parsedStock() {
    return Number(this.stockText)
  }

  get isComplete() {
    return Boolean(
      this.title &&
        this.author &&
        this.isbn &&
        this.category &&
        this.stockText &&
        this.summary,
    )
  }

  get hasValidStock() {
    return Number.isInteger(this.parsedStock) && this.parsedStock > 0
  }
}

export interface SaveBookRequest {
  draft: BookFormDraft
}

export const SaveBookFailureReasons = {
  DuplicateIsbn: 'duplicate_isbn',
  InvalidStock: 'invalid_stock',
  InvalidForm: 'invalid_form',
} as const

export type SaveBookFailureReason =
  (typeof SaveBookFailureReasons)[keyof typeof SaveBookFailureReasons]

export type SaveBookResponse =
  | {
      ok: true
      bookId: string
      redirectTo: string
    }
  | {
      ok: false
      reason: SaveBookFailureReason
      message: string
    }
