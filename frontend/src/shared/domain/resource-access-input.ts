import type { AccessSubject, BaseAccess, ResourceAccessPolicy } from '@/shared/domain/resource-lifecycle'
import type { Username } from '@/features/user/model/Username'
import type { UserGroupSlug } from '@/features/usergroup/model/UserGroupSlug'
import type { ParseResult } from '@/shared/domain/parsing'

type AccessPolicyBuildResult =
  | { ok: true; value: ResourceAccessPolicy }
  | { ok: false; message: string }

type AccessSubjectParsers = {
  parseUsername: (token: string) => ParseResult<Username>
  parseUserGroupSlug: (token: string) => ParseResult<UserGroupSlug>
}

export function buildResourceAccessPolicy(
  parsers: AccessSubjectParsers,
  baseAccess: BaseAccess,
  grantedUsersInput: string,
  grantedGroupsInput: string,
  grantedManagerUsersInput = '',
  grantedManagerGroupsInput = '',
): AccessPolicyBuildResult {
  const parsedUsers = parseSubjects(
    parseAccessSubjectInput(grantedUsersInput),
    parsers.parseUsername,
    (username) => ({ kind: 'user' as const, username }),
  )
  if (!parsedUsers.ok) {
    return parsedUsers
  }

  const parsedGroups = parseSubjects(
    parseAccessSubjectInput(grantedGroupsInput),
    parsers.parseUserGroupSlug,
    (slug) => ({ kind: 'user_group' as const, slug }),
  )
  if (!parsedGroups.ok) {
    return parsedGroups
  }

  const parsedManagerUsers = parseSubjects(
    parseAccessSubjectInput(grantedManagerUsersInput),
    parsers.parseUsername,
    (username) => ({ kind: 'user' as const, username }),
  )
  if (!parsedManagerUsers.ok) {
    return parsedManagerUsers
  }

  const parsedManagerGroups = parseSubjects(
    parseAccessSubjectInput(grantedManagerGroupsInput),
    parsers.parseUserGroupSlug,
    (slug) => ({ kind: 'user_group' as const, slug }),
  )
  if (!parsedManagerGroups.ok) {
    return parsedManagerGroups
  }

  return {
    ok: true,
    value: {
      baseAccess,
      viewerGrants: dedupeAccessSubjects([...parsedGroups.value, ...parsedUsers.value]),
      managerGrants: dedupeAccessSubjects([...parsedManagerGroups.value, ...parsedManagerUsers.value]),
    },
  }
}

export function grantedUsersInputFromAccessPolicy(accessPolicy: ResourceAccessPolicy): string {
  return accessPolicy.viewerGrants
    .filter((grant): grant is Extract<AccessSubject, { kind: 'user' }> => grant.kind === 'user')
    .map((grant) => grant.username)
    .join('\n')
}

export function grantedGroupsInputFromAccessPolicy(accessPolicy: ResourceAccessPolicy): string {
  return accessPolicy.viewerGrants
    .filter((grant): grant is Extract<AccessSubject, { kind: 'user_group' }> => grant.kind === 'user_group')
    .map((grant) => grant.slug)
    .join('\n')
}

export function grantedManagerUsersInputFromAccessPolicy(accessPolicy: ResourceAccessPolicy): string {
  return accessPolicy.managerGrants
    .filter((grant): grant is Extract<AccessSubject, { kind: 'user' }> => grant.kind === 'user')
    .map((grant) => grant.username)
    .join('\n')
}

export function grantedManagerGroupsInputFromAccessPolicy(accessPolicy: ResourceAccessPolicy): string {
  return accessPolicy.managerGrants
    .filter((grant): grant is Extract<AccessSubject, { kind: 'user_group' }> => grant.kind === 'user_group')
    .map((grant) => grant.slug)
    .join('\n')
}

export function normalizeAccessSubjectInput(raw: string): string {
  return parseAccessSubjectInput(raw).join('\n')
}

function parseAccessSubjectInput(raw: string): string[] {
  return raw
    .split(/[\n,]/)
    .map((token) => token.trim())
    .filter((token) => token.length > 0)
}

function dedupeAccessSubjects(subjects: AccessSubject[]): AccessSubject[] {
  const seen = new Set<string>()
  return subjects.filter((subject) => {
    const key = subject.kind === 'user' ? `user:${subject.username}` : `user_group:${subject.slug}`
    if (seen.has(key)) {
      return false
    }
    seen.add(key)
    return true
  })
}

function parseSubjects<TParsed, TSubject extends AccessSubject>(
  tokens: string[],
  parse: (token: string) => { ok: true; value: TParsed } | { ok: false; error: string },
  toSubject: (value: TParsed) => TSubject,
): { ok: true; value: TSubject[] } | { ok: false; message: string } {
  return tokens.reduce<{ ok: true; value: TSubject[] } | { ok: false; message: string }>((acc, token) => {
    if (!acc.ok) {
      return acc
    }

    const result = parse(token)
    if (!result.ok) {
      return { ok: false, message: result.error }
    }

    return { ok: true, value: [...acc.value, toSubject(result.value)] }
  }, { ok: true, value: [] })
}
