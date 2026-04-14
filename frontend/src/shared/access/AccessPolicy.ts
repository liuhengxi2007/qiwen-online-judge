export type ResourceId = string & { readonly __brand: 'ResourceId' }

export type ResourceKind = 'problem' | 'problem_set'

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

export type GrantRole = 'viewer' | 'manager'

export type ResourceAccessGrant = {
  resourceKind: ResourceKind
  resourceId: ResourceId
  grantRole: GrantRole
  subject: AccessSubject
  createdAt: string
}
