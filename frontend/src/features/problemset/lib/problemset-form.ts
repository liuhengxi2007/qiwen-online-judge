import { parseProblemSetDescription, parseProblemSetSlug, parseProblemSetTitle } from '@/features/problemset/lib/problemset-parsers'
import type { CreateProblemSetRequest } from '@/features/problemset/model/request/CreateProblemSetRequest'
import type { UpdateProblemSetRequest } from '@/features/problemset/model/request/UpdateProblemSetRequest'
import { buildResourceAccessPolicy } from '@/shared/domain/resource-access-input'
import { resourceAccessSubjectParsers } from '@/shared/domain/access/access-subject-parsers'
import type { BaseAccess } from '@/shared/domain/resource-lifecycle'

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

  const accessPolicyResult = buildResourceAccessPolicy(resourceAccessSubjectParsers, draft.baseAccess, draft.grantedUsersInput, draft.grantedGroupsInput)
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

  const accessPolicyResult = buildResourceAccessPolicy(resourceAccessSubjectParsers, draft.baseAccess, draft.grantedUsersInput, draft.grantedGroupsInput)
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
