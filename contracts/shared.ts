export type BaseAccess = 'owner_only' | 'public'

export type UserAccessSubject = {
  kind: 'user'
  username: string
}

export type UserGroupAccessSubject = {
  kind: 'user_group'
  slug: string
}

export type AccessSubject = UserAccessSubject | UserGroupAccessSubject

export type ResourceAccessPolicy = {
  baseAccess: BaseAccess
  viewerGrants: AccessSubject[]
  managerGrants: AccessSubject[]
}

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
