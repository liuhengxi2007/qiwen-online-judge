/** 分页响应容器；承载当前页条目和后端统计的总条目数。 */
export type PageResponse<TItem> = {
  items: TItem[]
  page: number
  pageSize: number
  totalItems: number
}
