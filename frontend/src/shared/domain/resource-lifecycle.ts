import type {
  AccessSubject as ContractAccessSubject,
  BaseAccess as ContractBaseAccess,
  ResourceAccessPolicy as ContractResourceAccessPolicy,
  ResourceStatus as ContractResourceStatus,
} from '@contracts/shared'

export type BaseAccess = ContractBaseAccess

export type UserAccessSubject = Extract<ContractAccessSubject, { kind: 'user' }>
export type UserGroupAccessSubject = Extract<ContractAccessSubject, { kind: 'user_group' }>
export type AccessSubject = ContractAccessSubject
export type ResourceAccessPolicy = ContractResourceAccessPolicy

export type ResourceStatus = ContractResourceStatus

export type AuditFields = {
  createdAt: string
  updatedAt: string
}

export function createOwnerOnlyAccessPolicy(): ResourceAccessPolicy {
  return {
    baseAccess: 'owner_only',
    viewerGrants: [],
  }
}

export function resourceAccessBadgeLabel(accessPolicy: ResourceAccessPolicy): string {
  if (accessPolicy.baseAccess === 'public') {
    return 'Public'
  }

  return accessPolicy.viewerGrants.length > 0 ? 'Shared' : 'Private'
}

export function resourceAccessSummary(accessPolicy: ResourceAccessPolicy): string {
  const directUsers = accessPolicy.viewerGrants.filter((grant: AccessSubject) => grant.kind === 'user').length
  const userGroups = accessPolicy.viewerGrants.filter((grant: AccessSubject) => grant.kind === 'user_group').length

  if (accessPolicy.baseAccess === 'public') {
    return 'Visible to all signed-in users.'
  }

  if (directUsers === 0 && userGroups === 0) {
    return 'Visible only to the owner and global managers.'
  }

  const parts: string[] = []
  if (userGroups > 0) {
    parts.push(`${userGroups} group${userGroups === 1 ? '' : 's'}`)
  }
  if (directUsers > 0) {
    parts.push(`${directUsers} user${directUsers === 1 ? '' : 's'}`)
  }

  return `Shared with ${parts.join(' and ')}.`
}
