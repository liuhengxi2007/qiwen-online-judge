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

export type ApiMessageParam =
  | { kind: 'text'; value: string }
  | { kind: 'int'; value: number }
  | { kind: 'long'; value: number }
  | { kind: 'bool'; value: boolean }

export type ErrorResponse = {
  code: string | null
  message: string | null
  params: Record<string, ApiMessageParam>
}

export type SuccessResponse = {
  code: string | null
  message: string | null
  params: Record<string, ApiMessageParam>
}

export type PageResponse<TItem> = {
  items: TItem[]
  page: number
  pageSize: number
  totalItems: number
}
