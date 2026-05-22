export type { PageRequest } from '@/shared/model/PageRequest'
export type { PageResponse } from '@/shared/model/PageResponse'
import type { PageRequest } from '@/shared/model/PageRequest'

export const DEFAULT_PAGE = 1
export const DEFAULT_PAGE_SIZE = 20
export const DEFAULT_PAGE_WINDOW_RADIUS = 2

export type PageCorrection =
  | { kind: 'none' }
  | { kind: 'delete' }
  | { kind: 'set'; page: number }

export function normalizePageRequest(pageRequest: Partial<PageRequest>): PageRequest {
  const page = Number.isInteger(pageRequest.page) && (pageRequest.page ?? 0) > 0 ? pageRequest.page! : DEFAULT_PAGE
  const pageSize =
    Number.isInteger(pageRequest.pageSize) && (pageRequest.pageSize ?? 0) > 0
      ? pageRequest.pageSize!
      : DEFAULT_PAGE_SIZE

  return { page, pageSize }
}

export function parsePositivePage(value: string | null): number {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : DEFAULT_PAGE
}

export function calculateTotalPages(totalItems: number, pageSize: number): number {
  return Math.max(1, Math.ceil(totalItems / pageSize))
}

export function buildPageNumbers(
  currentPage: number,
  totalPages: number,
  radius = DEFAULT_PAGE_WINDOW_RADIUS,
): number[] {
  const firstPage = Math.max(1, currentPage - radius)
  const lastPage = Math.min(totalPages, currentPage + radius)
  return Array.from({ length: lastPage - firstPage + 1 }, (_, index) => firstPage + index)
}

export function getPageCorrection(currentPage: number, totalPages: number): PageCorrection {
  if (currentPage <= totalPages) {
    return { kind: 'none' }
  }

  if (totalPages <= 1) {
    return { kind: 'delete' }
  }

  return { kind: 'set', page: totalPages }
}
