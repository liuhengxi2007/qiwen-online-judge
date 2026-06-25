import type { AccessSubject } from '@/objects/shared/access/AccessSubject'
import { parseAccessUserGroupSlug } from '@/objects/shared/access/AccessUserGroupSlug'
import { parseAccessUsername } from '@/objects/shared/access/AccessUsername'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'
import type { ResourceVisibilityPolicy } from '@/objects/shared/access/ResourceVisibilityPolicy'

/**
 * 访问策略构造结果，成功时返回结构化策略，失败时返回首个校验错误。
 */
type AccessPolicyBuildResult =
  | { ok: true; value: ResourceAccessPolicy }
  | { ok: false; message: string }

type VisibilityPolicyBuildResult =
  | { ok: true; value: ResourceVisibilityPolicy }
  | { ok: false; message: string }

type ResourceGrantInput = {
  usersInput: string
  groupsInput: string
}

type ResourceAccessPolicyInput = {
  baseAccess: BaseAccess
  viewer: ResourceGrantInput
  manager?: ResourceGrantInput
}

/**
 * 从文本框输入构造资源访问策略；支持换行或逗号分隔主体，并去重 viewer/manager 授权。
 */
export function buildResourceAccessPolicy(input: ResourceAccessPolicyInput): AccessPolicyBuildResult {
  const manager = input.manager ?? { usersInput: '', groupsInput: '' }
  const parsedUsers = parseSubjects(
    parseAccessSubjectInput(input.viewer.usersInput),
    parseAccessUsername,
    (username) => ({ kind: 'user' as const, username }),
  )
  if (!parsedUsers.ok) {
    return parsedUsers
  }

  const parsedGroups = parseSubjects(
    parseAccessSubjectInput(input.viewer.groupsInput),
    parseAccessUserGroupSlug,
    (slug) => ({ kind: 'user_group' as const, slug }),
  )
  if (!parsedGroups.ok) {
    return parsedGroups
  }

  const parsedManagerUsers = parseSubjects(
    parseAccessSubjectInput(manager.usersInput),
    parseAccessUsername,
    (username) => ({ kind: 'user' as const, username }),
  )
  if (!parsedManagerUsers.ok) {
    return parsedManagerUsers
  }

  const parsedManagerGroups = parseSubjects(
    parseAccessSubjectInput(manager.groupsInput),
    parseAccessUserGroupSlug,
    (slug) => ({ kind: 'user_group' as const, slug }),
  )
  if (!parsedManagerGroups.ok) {
    return parsedManagerGroups
  }

  return {
    ok: true,
    value: {
      baseAccess: input.baseAccess,
      viewerGrants: dedupeAccessSubjects([...parsedGroups.value, ...parsedUsers.value]),
      managerGrants: dedupeAccessSubjects([...parsedManagerGroups.value, ...parsedManagerUsers.value]),
    },
  }
}

/**
 * 从文本框输入构造 viewer-only 资源可见性策略；不包含资源级管理授权。
 */
export function buildResourceVisibilityPolicy(
  baseAccess: BaseAccess,
  grantedUsersInput: string,
  grantedGroupsInput: string,
): VisibilityPolicyBuildResult {
  const parsedUsers = parseSubjects(
    parseAccessSubjectInput(grantedUsersInput),
    parseAccessUsername,
    (username) => ({ kind: 'user' as const, username }),
  )
  if (!parsedUsers.ok) {
    return parsedUsers
  }

  const parsedGroups = parseSubjects(
    parseAccessSubjectInput(grantedGroupsInput),
    parseAccessUserGroupSlug,
    (slug) => ({ kind: 'user_group' as const, slug }),
  )
  if (!parsedGroups.ok) {
    return parsedGroups
  }

  return {
    ok: true,
    value: {
      baseAccess,
      viewerGrants: dedupeAccessSubjects([...parsedGroups.value, ...parsedUsers.value]),
    },
  }
}

/**
 * 从访问策略提取 viewer 用户授权，输出适合文本框展示的逐行用户名。
 */
export function grantedUsersInputFromAccessPolicy(accessPolicy: ResourceAccessPolicy): string {
  return grantedUsersInputFromVisibilityPolicy(accessPolicy)
}

/**
 * 从可见性策略提取 viewer 用户授权，输出适合文本框展示的逐行用户名。
 */
export function grantedUsersInputFromVisibilityPolicy(accessPolicy: ResourceVisibilityPolicy): string {
  return accessPolicy.viewerGrants
    .filter((grant): grant is Extract<AccessSubject, { kind: 'user' }> => grant.kind === 'user')
    .map((grant) => grant.username)
    .join('\n')
}

/**
 * 从访问策略提取 viewer 用户组授权，输出适合文本框展示的逐行用户组 slug。
 */
export function grantedGroupsInputFromAccessPolicy(accessPolicy: ResourceAccessPolicy): string {
  return grantedGroupsInputFromVisibilityPolicy(accessPolicy)
}

/**
 * 从可见性策略提取 viewer 用户组授权，输出适合文本框展示的逐行用户组 slug。
 */
export function grantedGroupsInputFromVisibilityPolicy(accessPolicy: ResourceVisibilityPolicy): string {
  return accessPolicy.viewerGrants
    .filter((grant): grant is Extract<AccessSubject, { kind: 'user_group' }> => grant.kind === 'user_group')
    .map((grant) => grant.slug)
    .join('\n')
}

/**
 * 从访问策略提取 manager 用户授权，输出适合文本框展示的逐行用户名。
 */
export function grantedManagerUsersInputFromAccessPolicy(accessPolicy: ResourceAccessPolicy): string {
  return accessPolicy.managerGrants
    .filter((grant): grant is Extract<AccessSubject, { kind: 'user' }> => grant.kind === 'user')
    .map((grant) => grant.username)
    .join('\n')
}

/**
 * 从访问策略提取 manager 用户组授权，输出适合文本框展示的逐行用户组 slug。
 */
export function grantedManagerGroupsInputFromAccessPolicy(accessPolicy: ResourceAccessPolicy): string {
  return accessPolicy.managerGrants
    .filter((grant): grant is Extract<AccessSubject, { kind: 'user_group' }> => grant.kind === 'user_group')
    .map((grant) => grant.slug)
    .join('\n')
}

/**
 * 规范化授权主体输入，把逗号/换行分隔的非空 token 统一为逐行文本。
 */
export function normalizeAccessSubjectInput(raw: string): string {
  return parseAccessSubjectInput(raw).join('\n')
}

/**
 * 解析授权主体原始输入，按逗号或换行切分并去掉空白 token。
 */
function parseAccessSubjectInput(raw: string): string[] {
  return raw
    .split(/[\n,]/)
    .map((token) => token.trim())
    .filter((token) => token.length > 0)
}

/**
 * 按主体类型和标识去重访问授权，保留首次出现顺序。
 */
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

/**
 * 批量解析授权主体 token；遇到第一个非法 token 时停止并返回错误消息。
 */
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
