import { parseProblemSetDescription, problemSetDescriptionValue } from '@/objects/problemset/ProblemSetDescription'
import { parseProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { parseProblemSetTitle, problemSetTitleValue } from '@/objects/problemset/ProblemSetTitle'
import type { CreateProblemSetRequest } from '@/objects/problemset/request/CreateProblemSetRequest'
import type { UpdateProblemSetRequest } from '@/objects/problemset/request/UpdateProblemSetRequest'
import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import {
  buildResourceAccessPolicy,
  grantedGroupsInputFromAccessPolicy,
  grantedUsersInputFromAccessPolicy,
} from '@/pages/objects/resource-access-input'

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

  const accessPolicyResult = buildResourceAccessPolicy(
    draft.baseAccess,
    draft.grantedUsersInput,
    draft.grantedGroupsInput,
  )
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

type ProblemSetAccessEditorState = {
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
}

type ProblemSetContentEditorState = ProblemSetAccessEditorState & {
  title: string
  description: string
}

export function buildProblemSetContentUpdateDraft(
  problemSet: ProblemSetDetail,
  editor: ProblemSetContentEditorState,
): UpdateProblemSetDraft {
  return {
    title: editor.title,
    description: editor.description,
    baseAccess: problemSet.accessPolicy.baseAccess,
    grantedUsersInput: grantedUsersInputFromAccessPolicy(problemSet.accessPolicy),
    grantedGroupsInput: grantedGroupsInputFromAccessPolicy(problemSet.accessPolicy),
  }
}

export function buildProblemSetAccessUpdateDraft(
  problemSet: ProblemSetDetail,
  editor: ProblemSetAccessEditorState,
): UpdateProblemSetDraft {
  return {
    title: problemSetTitleValue(problemSet.title),
    description: problemSetDescriptionValue(problemSet.description),
    baseAccess: editor.baseAccess,
    grantedUsersInput: editor.grantedUsersInput,
    grantedGroupsInput: editor.grantedGroupsInput,
  }
}

export function buildProblemSetDetailAccessPolicy(editor: ProblemSetAccessEditorState) {
  const accessPolicyResult = buildResourceAccessPolicy(
    editor.baseAccess,
    editor.grantedUsersInput,
    editor.grantedGroupsInput,
  )

  return accessPolicyResult.ok
    ? accessPolicyResult.value
    : { baseAccess: editor.baseAccess, viewerGrants: [], managerGrants: [] }
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

  const accessPolicyResult = buildResourceAccessPolicy(
    draft.baseAccess,
    draft.grantedUsersInput,
    draft.grantedGroupsInput,
  )
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
