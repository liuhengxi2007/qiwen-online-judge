import type { PageResponse as ContractPageResponse } from '@contracts/shared'

export type PageRequest = {
  page: number
  pageSize: number
}

export type PageResponse<TItem> = ContractPageResponse<TItem>

export const DEFAULT_PAGE = 1
export const DEFAULT_PAGE_SIZE = 20

export function normalizePageRequest(pageRequest: Partial<PageRequest>): PageRequest {
  const page = Number.isInteger(pageRequest.page) && (pageRequest.page ?? 0) > 0 ? pageRequest.page! : DEFAULT_PAGE
  const pageSize =
    Number.isInteger(pageRequest.pageSize) && (pageRequest.pageSize ?? 0) > 0
      ? pageRequest.pageSize!
      : DEFAULT_PAGE_SIZE

  return { page, pageSize }
}
