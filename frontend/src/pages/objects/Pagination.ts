/**
 * 页面层复用共享分页请求类型的转出口，保持旧导入路径兼容。
 */
export type { PageRequest } from '@/objects/shared/PageRequest'
/**
 * 页面层复用共享分页响应类型的转出口，保持旧导入路径兼容。
 */
export type { PageResponse } from '@/objects/shared/PageResponse'
import type { PageRequest } from '@/objects/shared/PageRequest'

/**
 * 默认页码，页面参数缺失或非法时使用第一页。
 */
export const DEFAULT_PAGE = 1
/**
 * 默认每页数量，供通用分页请求归一化使用。
 */
export const DEFAULT_PAGE_SIZE = 20
/**
 * 当前页前后展示的页码窗口半径。
 */
export const DEFAULT_PAGE_WINDOW_RADIUS = 3

/**
 * URL 页码修正结果，描述无需修正、删除 page 参数或设置到指定页。
 */
export type PageCorrection =
  | { kind: 'none' }
  | { kind: 'delete' }
  | { kind: 'set'; page: number }

/**
 * 归一化分页请求，确保 page 和 pageSize 都是正整数，否则回退默认值。
 */
export function normalizePageRequest(pageRequest: Partial<PageRequest>): PageRequest {
  const page = Number.isInteger(pageRequest.page) && (pageRequest.page ?? 0) > 0 ? pageRequest.page! : DEFAULT_PAGE
  const pageSize =
    Number.isInteger(pageRequest.pageSize) && (pageRequest.pageSize ?? 0) > 0
      ? pageRequest.pageSize!
      : DEFAULT_PAGE_SIZE

  return { page, pageSize }
}

/**
 * 从 URL 参数解析正整数页码，解析失败时返回默认第一页。
 */
export function parsePositivePage(value: string | null): number {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : DEFAULT_PAGE
}

/**
 * 根据总条目数和每页数量计算总页数，最少返回 1 以保持分页 UI 稳定。
 */
export function calculateTotalPages(totalItems: number, pageSize: number): number {
  return Math.max(1, Math.ceil(totalItems / pageSize))
}

/**
 * 生成当前页附近的页码窗口，输入为当前页、总页数和可选窗口半径。
 */
export function buildPageNumbers(
  currentPage: number,
  totalPages: number,
  radius = DEFAULT_PAGE_WINDOW_RADIUS,
): number[] {
  const firstPage = Math.max(1, currentPage - radius)
  const lastPage = Math.min(totalPages, currentPage + radius)
  return Array.from({ length: lastPage - firstPage + 1 }, (_, index) => firstPage + index)
}

/**
 * 判断当前页是否越界，并给出 URL page 参数的修正策略。
 */
export function getPageCorrection(currentPage: number, totalPages: number): PageCorrection {
  if (currentPage <= totalPages) {
    return { kind: 'none' }
  }

  if (totalPages <= 1) {
    return { kind: 'delete' }
  }

  return { kind: 'set', page: totalPages }
}
