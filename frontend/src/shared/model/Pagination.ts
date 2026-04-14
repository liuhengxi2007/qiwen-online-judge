export type PageRequest = {
  page: number
  pageSize: number
}

export type PageResponse<TItem> = {
  items: TItem[]
  page: number
  pageSize: number
  totalItems: number
}
