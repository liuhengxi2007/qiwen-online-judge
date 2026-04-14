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

type Translate = (key: string, values?: Record<string, string | number>) => string

export function createOwnerOnlyAccessPolicy(): ResourceAccessPolicy {
  return {
    baseAccess: 'owner_only',
    viewerGrants: [],
    managerGrants: [],
  }
}

export function resourceAccessBadgeLabel(accessPolicy: ResourceAccessPolicy, t?: Translate): string {
  if (accessPolicy.baseAccess === 'public') {
    return t ? t('resourceAccess.badge.public') : 'Public'
  }

  return accessPolicy.viewerGrants.length > 0
    ? (t ? t('resourceAccess.badge.shared') : 'Shared')
    : (t ? t('resourceAccess.badge.private') : 'Private')
}

export function resourceAccessSummary(accessPolicy: ResourceAccessPolicy, t?: Translate): string {
  const directUsers = accessPolicy.viewerGrants.filter((grant: AccessSubject) => grant.kind === 'user').length
  const userGroups = accessPolicy.viewerGrants.filter((grant: AccessSubject) => grant.kind === 'user_group').length
  const managerUsers = accessPolicy.managerGrants.filter((grant: AccessSubject) => grant.kind === 'user').length
  const managerGroups = accessPolicy.managerGrants.filter((grant: AccessSubject) => grant.kind === 'user_group').length

  const visibilitySummary =
    accessPolicy.baseAccess === 'public'
      ? (t ? t('resourceAccess.summary.visibleAll') : 'Visible to all signed-in users.')
      : directUsers === 0 && userGroups === 0
        ? (t ? t('resourceAccess.summary.visibleGrantedOnly') : 'Visible only to explicitly granted viewers and global managers.')
        : (t
            ? t('resourceAccess.summary.sharedWith', { summary: formatGrantSummary(userGroups, directUsers, t) })
            : `Shared with ${formatGrantSummary(userGroups, directUsers)}.`)

  if (managerUsers === 0 && managerGroups === 0) {
    return `${visibilitySummary} ${t ? t('resourceAccess.summary.managedGlobalOnly') : 'Managed only by global managers.'}`
  }

  return `${visibilitySummary} ${
    t
      ? t('resourceAccess.summary.managedBy', { summary: formatGrantSummary(managerGroups, managerUsers, t) })
      : `Managed by ${formatGrantSummary(managerGroups, managerUsers)} and global managers.`
  }`
}

function formatGrantSummary(groupCount: number, userCount: number, t?: Translate): string {
  const parts: string[] = []
  if (groupCount > 0) {
    parts.push(
      t
        ? t(groupCount === 1 ? 'resourceAccess.summary.group.one' : 'resourceAccess.summary.group.other', {
            count: groupCount,
          })
        : `${groupCount} group${groupCount === 1 ? '' : 's'}`,
    )
  }
  if (userCount > 0) {
    parts.push(
      t
        ? t(userCount === 1 ? 'resourceAccess.summary.user.one' : 'resourceAccess.summary.user.other', {
            count: userCount,
          })
        : `${userCount} user${userCount === 1 ? '' : 's'}`,
    )
  }
  return parts.length === 2 && t ? t('resourceAccess.summary.join', { left: parts[0], right: parts[1] }) : parts.join(' and ')
}
