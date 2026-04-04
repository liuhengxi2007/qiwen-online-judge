import { parseUsername } from '@/features/auth/domain/auth'
import {
  parseProblemSetDescription,
  parseProblemSetSlug,
  parseProblemSetTitle,
  type CreateProblemSetRequest,
  type UpdateProblemSetRequest,
} from '@/features/problemset/domain/problemset'
import { parseUserGroupSlug } from '@/features/usergroup/domain/usergroup'
import type { AccessSubject, BaseAccess } from '@/shared/domain/resource-lifecycle'

export type ProblemSetDraft = {
  slug: string
  title: string
  description: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
}

export function validateProblemSetDraft(
  draft: ProblemSetDraft,
): { ok: true; request: CreateProblemSetRequest } | { ok: false; message: string } {
  const slugResult = parseProblemSetSlug(draft.slug)
  if (!slugResult.ok) {
    return { ok: false, message: slugResult.error }
  }

  const titleResult = parseProblemSetTitle(draft.title)
  if (!titleResult.ok) {
    return { ok: false, message: titleResult.error }
  }

  const descriptionResult = parseProblemSetDescription(draft.description)
  if (!descriptionResult.ok) {
    return { ok: false, message: descriptionResult.error }
  }

  const accessPolicyResult = buildAccessPolicy(draft.baseAccess, draft.grantedUsersInput, draft.grantedGroupsInput)
  if (!accessPolicyResult.ok) {
    return { ok: false, message: accessPolicyResult.message }
  }

  return {
    ok: true,
    request: {
      slug: slugResult.value,
      title: titleResult.value,
      description: descriptionResult.value,
      accessPolicy: accessPolicyResult.value,
    },
  }
}

export type UpdateProblemSetDraft = {
  title: string
  description: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
}

export function validateProblemSetUpdateDraft(
  draft: UpdateProblemSetDraft,
): { ok: true; request: UpdateProblemSetRequest } | { ok: false; message: string } {
  const titleResult = parseProblemSetTitle(draft.title)
  if (!titleResult.ok) {
    return { ok: false, message: titleResult.error }
  }

  const descriptionResult = parseProblemSetDescription(draft.description)
  if (!descriptionResult.ok) {
    return { ok: false, message: descriptionResult.error }
  }

  const accessPolicyResult = buildAccessPolicy(draft.baseAccess, draft.grantedUsersInput, draft.grantedGroupsInput)
  if (!accessPolicyResult.ok) {
    return { ok: false, message: accessPolicyResult.message }
  }

  return {
    ok: true,
    request: {
      title: titleResult.value,
      description: descriptionResult.value,
      accessPolicy: accessPolicyResult.value,
    },
  }
}

function buildAccessPolicy(
  baseAccess: BaseAccess,
  grantedUsersInput: string,
  grantedGroupsInput: string,
): { ok: true; value: { baseAccess: BaseAccess; viewerGrants: AccessSubject[] } } | { ok: false; message: string } {
  const users = parseTokenList(grantedUsersInput)
  const groups = parseTokenList(grantedGroupsInput)

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
      viewerGrants: dedupeSubjects([...parsedGroups, ...parsedUsers]),
    },
  }
}

function parseTokenList(raw: string): string[] {
  return raw
    .split(/[\n,]/)
    .map((token) => token.trim())
    .filter((token) => token.length > 0)
}

function dedupeSubjects(subjects: AccessSubject[]): AccessSubject[] {
  const seen = new Set<string>()
  return subjects.filter((subject) => {
    const key =
      subject.kind === 'user' ? `user:${subject.username}` : `user_group:${subject.slug}`
    if (seen.has(key)) {
      return false
    }
    seen.add(key)
    return true
  })
}
