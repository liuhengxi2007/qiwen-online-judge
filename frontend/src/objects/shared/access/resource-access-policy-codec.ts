import {
  accessUserGroupSlugValue,
  accessUsernameValue,
  parseUserGroupSlug,
  parseUsername,
} from '@/objects/shared/access/access-subject-parsers'
import type { AccessSubject, BaseAccess, ResourceAccessPolicy } from '@/objects/shared/access/AccessPolicy'
import { requireParsed } from '@/objects/shared/parsing'

type AccessSubjectContract =
  | {
      kind: 'user'
      username: string
    }
  | {
      kind: 'user_group'
      slug: string
    }

type ResourceAccessPolicyContract = {
  baseAccess: BaseAccess
  viewerGrants: AccessSubjectContract[]
  managerGrants: AccessSubjectContract[]
}

export function fromResourceAccessPolicyContract(value: unknown): ResourceAccessPolicy {
  const policy = readRecord(value, 'resource access policy')

  return {
    baseAccess: readBaseAccess(policy.baseAccess),
    viewerGrants: readSubjectArray(policy.viewerGrants, 'resource access viewer grant'),
    managerGrants: readSubjectArray(policy.managerGrants, 'resource access manager grant'),
  }
}

export function toResourceAccessPolicyContract(policy: ResourceAccessPolicy): ResourceAccessPolicyContract {
  return {
    baseAccess: policy.baseAccess,
    viewerGrants: policy.viewerGrants.map(toAccessSubjectContract),
    managerGrants: policy.managerGrants.map(toAccessSubjectContract),
  }
}

function toAccessSubjectContract(subject: AccessSubject): AccessSubjectContract {
  switch (subject.kind) {
    case 'user':
      return {
        kind: 'user',
        username: accessUsernameValue(subject.username),
      }
    case 'user_group':
      return {
        kind: 'user_group',
        slug: accessUserGroupSlugValue(subject.slug),
      }
  }
}

function readSubjectArray(value: unknown, label: string): AccessSubject[] {
  if (!Array.isArray(value)) {
    throw new Error(`Invalid ${label}s.`)
  }

  return value.map((subject) => readAccessSubject(subject, label))
}

function readAccessSubject(value: unknown, label: string): AccessSubject {
  const subject = readRecord(value, label)
  const kind = readString(subject.kind, `${label} kind`)

  switch (kind) {
    case 'user':
      return {
        kind: 'user',
        username: requireParsed(parseUsername(readString(subject.username, `${label} username`)), `${label} username`),
      }
    case 'user_group':
      return {
        kind: 'user_group',
        slug: requireParsed(parseUserGroupSlug(readString(subject.slug, `${label} slug`)), `${label} slug`),
      }
    default:
      throw new Error(`Unknown ${label} kind: ${kind}.`)
  }
}

function readBaseAccess(value: unknown): BaseAccess {
  if (value === 'owner_only' || value === 'public') {
    return value
  }

  throw new Error('Invalid resource access base access.')
}

function readRecord(value: unknown, field: string): Record<string, unknown> {
  if (typeof value !== 'object' || value === null) {
    throw new Error(`Invalid ${field}.`)
  }

  return value as Record<string, unknown>
}

function readString(value: unknown, field: string): string {
  if (typeof value !== 'string') {
    throw new Error(`Invalid ${field}.`)
  }

  return value
}
