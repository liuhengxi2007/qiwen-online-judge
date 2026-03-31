export type ResourceVisibility = 'private' | 'group' | 'public'

export type ResourceStatus = 'draft' | 'published' | 'archived'

export type ErrorResponse = {
  message: string
}

export type SuccessResponse = {
  message: string
}

export type PageResponse<TItem> = {
  items: TItem[]
  page: number
  pageSize: number
  totalItems: number
}
