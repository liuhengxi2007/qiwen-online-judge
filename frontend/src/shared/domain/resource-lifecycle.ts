import type {
  AccessSubject as ContractAccessSubject,
  BaseAccess as ContractBaseAccess,
  ResourceAccessPolicy as ContractResourceAccessPolicy,
} from '@contracts/shared'

export type BaseAccess = ContractBaseAccess

export type UserAccessSubject = Extract<ContractAccessSubject, { kind: 'user' }>
export type UserGroupAccessSubject = Extract<ContractAccessSubject, { kind: 'user_group' }>
export type AccessSubject = ContractAccessSubject
export type ResourceAccessPolicy = ContractResourceAccessPolicy

export type AuditFields = {
  createdAt: string
  updatedAt: string
}

export function createOwnerOnlyAccessPolicy(): ResourceAccessPolicy {
  return {
    baseAccess: 'owner_only',
    viewerGrants: [],
    managerGrants: [],
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
  const managerUsers = accessPolicy.managerGrants.filter((grant: AccessSubject) => grant.kind === 'user').length
  const managerGroups = accessPolicy.managerGrants.filter((grant: AccessSubject) => grant.kind === 'user_group').length

  const visibilitySummary =
    accessPolicy.baseAccess === 'public'
      ? 'Visible to all signed-in users.'
      : directUsers === 0 && userGroups === 0
        ? 'Visible only to explicitly granted viewers and global managers.'
        : `Shared with ${formatGrantSummary(userGroups, directUsers)}.`

  if (managerUsers === 0 && managerGroups === 0) {
    return `${visibilitySummary} Managed only by global managers.`
  }

  return `${visibilitySummary} Managed by ${formatGrantSummary(managerGroups, managerUsers)} and global managers.`
}

function formatGrantSummary(groupCount: number, userCount: number): string {
  const parts: string[] = []
  if (groupCount > 0) {
    parts.push(`${groupCount} group${groupCount === 1 ? '' : 's'}`)
  }
  if (userCount > 0) {
    parts.push(`${userCount} user${userCount === 1 ? '' : 's'}`)
  }
  return parts.join(' and ')
}
