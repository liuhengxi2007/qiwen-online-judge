import type { CreateProblemRequest, UpdateProblemRequest } from '@/features/problem/domain/problem'
import {
  parseProblemSlug,
  parseProblemStatementText,
  parseProblemTitle,
} from '@/features/problem/domain/problem'
import { parseUsername } from '@/features/auth/domain/auth'
import { parseUserGroupSlug } from '@/features/usergroup/domain/usergroup'
import type { AccessSubject, BaseAccess } from '@/shared/domain/resource-lifecycle'

type ProblemDraft = {
  slug: string
  title: string
  statement: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
}

type ProblemDraftValidation =
  | { ok: true; request: CreateProblemRequest }
  | { ok: false; message: string }

export function validateProblemDraft(draft: ProblemDraft): ProblemDraftValidation {
  const slugResult = parseProblemSlug(draft.slug)
  if (!slugResult.ok) {
    return { ok: false, message: slugResult.error }
  }

  const titleResult = parseProblemTitle(draft.title)
  if (!titleResult.ok) {
    return { ok: false, message: titleResult.error }
  }

  const statementResult = parseProblemStatementText(draft.statement)
  if (!statementResult.ok) {
    return { ok: false, message: statementResult.error }
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
      statement: statementResult.value,
      accessPolicy: accessPolicyResult.value,
    },
  }
}

export type UpdateProblemDraft = {
  title: string
  statement: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
}

export function validateProblemUpdateDraft(
  draft: UpdateProblemDraft,
): { ok: true; request: UpdateProblemRequest } | { ok: false; message: string } {
  const titleResult = parseProblemTitle(draft.title)
  if (!titleResult.ok) {
    return { ok: false, message: titleResult.error }
  }

  const statementResult = parseProblemStatementText(draft.statement)
  if (!statementResult.ok) {
    return { ok: false, message: statementResult.error }
  }

  const accessPolicyResult = buildAccessPolicy(draft.baseAccess, draft.grantedUsersInput, draft.grantedGroupsInput)
  if (!accessPolicyResult.ok) {
    return { ok: false, message: accessPolicyResult.message }
  }

  return {
    ok: true,
    request: {
      title: titleResult.value,
      statement: statementResult.value,
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
