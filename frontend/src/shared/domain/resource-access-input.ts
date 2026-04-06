import { parseUsername } from '@/features/auth/domain/auth'
import { parseUserGroupSlug } from '@/features/usergroup/domain/usergroup'
import type { AccessSubject, BaseAccess, ResourceAccessPolicy } from '@/shared/domain/resource-lifecycle'

type AccessPolicyBuildResult =
  | { ok: true; value: ResourceAccessPolicy }
  | { ok: false; message: string }

export function buildResourceAccessPolicy(
  baseAccess: BaseAccess,
  grantedUsersInput: string,
  grantedGroupsInput: string,
): AccessPolicyBuildResult {
  const users = parseAccessSubjectInput(grantedUsersInput)
  const groups = parseAccessSubjectInput(grantedGroupsInput)

  const parsedUsers: AccessSubject[] = []
  for (const token of users) {
    const result = parseUsername(token)
    if (!result.ok) {
      return { ok: false, message: result.error }
    }

    parsedUsers.push({ kind: 'user', username: result.value })
  }

  const parsedGroups: AccessSubject[] = []
  for (const token of groups) {
    const result = parseUserGroupSlug(token)
    if (!result.ok) {
      return { ok: false, message: result.error }
    }

    parsedGroups.push({ kind: 'user_group', slug: result.value })
  }

  return {
    ok: true,
    value: {
      baseAccess,
      viewerGrants: dedupeAccessSubjects([...parsedGroups, ...parsedUsers]),
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
