import type { CreateProblemRequest } from '@/objects/problem/request/CreateProblemRequest'
import type { OthersSubmissionAccess } from '@/objects/problem/OthersSubmissionAccess'
import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import { parseProblemSpaceLimitMb } from '@/objects/problem/ProblemSpaceLimitMb'
import { parseProblemStatementText, problemStatementTextValue } from '@/objects/problem/ProblemStatementText'
import { parseProblemTimeLimitMs } from '@/objects/problem/ProblemTimeLimitMs'
import { parseProblemTitle, problemTitleValue } from '@/objects/problem/ProblemTitle'
import type { UpdateProblemRequest } from '@/objects/problem/request/UpdateProblemRequest'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import {
  buildResourceAccessPolicy,
  grantedGroupsInputFromAccessPolicy,
  grantedManagerGroupsInputFromAccessPolicy,
  grantedManagerUsersInputFromAccessPolicy,
  grantedUsersInputFromAccessPolicy,
} from '@/pages/objects/resource-access-input'

type ProblemDraft = {
  slug: string
  title: string
  statement: string
  timeLimitMs: number
  spaceLimitMb: number
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
  managerUsersInput: string
  managerGroupsInput: string
  othersSubmissionAccess: OthersSubmissionAccess
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

  const timeLimitResult = parseProblemTimeLimitMs(draft.timeLimitMs)
  if (!timeLimitResult.ok) {
    return { ok: false, message: timeLimitResult.error }
  }

  const spaceLimitResult = parseProblemSpaceLimitMb(draft.spaceLimitMb)
  if (!spaceLimitResult.ok) {
    return { ok: false, message: spaceLimitResult.error }
  }

  const accessPolicyResult = buildResourceAccessPolicy(
    draft.baseAccess,
    draft.grantedUsersInput,
    draft.grantedGroupsInput,
    draft.managerUsersInput,
    draft.managerGroupsInput,
  )
  if (!accessPolicyResult.ok) {
    return { ok: false, message: accessPolicyResult.message }
  }

  return {
    ok: true,
    request: {
      slug: slugResult.value,
      title: titleResult.value,
      statement: statementResult.value,
      timeLimitMs: timeLimitResult.value,
      spaceLimitMb: spaceLimitResult.value,
      accessPolicy: accessPolicyResult.value,
      othersSubmissionAccess: draft.othersSubmissionAccess,
    },
  }
}

export type UpdateProblemDraft = {
  title: string
  statement: string
  timeLimitMs: number
  spaceLimitMb: number
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
  managerUsersInput: string
  managerGroupsInput: string
  othersSubmissionAccess: OthersSubmissionAccess
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

  const timeLimitResult = parseProblemTimeLimitMs(draft.timeLimitMs)
  if (!timeLimitResult.ok) {
    return { ok: false, message: timeLimitResult.error }
  }

  const spaceLimitResult = parseProblemSpaceLimitMb(draft.spaceLimitMb)
  if (!spaceLimitResult.ok) {
    return { ok: false, message: spaceLimitResult.error }
  }

  const accessPolicyResult = buildResourceAccessPolicy(
    draft.baseAccess,
    draft.grantedUsersInput,
    draft.grantedGroupsInput,
    draft.managerUsersInput,
    draft.managerGroupsInput,
  )
  if (!accessPolicyResult.ok) {
    return { ok: false, message: accessPolicyResult.message }
  }

  return {
    ok: true,
    request: {
      title: titleResult.value,
      statement: statementResult.value,
      timeLimitMs: timeLimitResult.value,
      spaceLimitMb: spaceLimitResult.value,
      accessPolicy: accessPolicyResult.value,
      othersSubmissionAccess: draft.othersSubmissionAccess,
    },
  }
}

type ProblemEditorAccessState = {
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
  managerUsersInput: string
  managerGroupsInput: string
  othersSubmissionAccess: OthersSubmissionAccess
}

type ProblemEditorContentState = ProblemEditorAccessState & {
  title: string
  statement: string
  timeLimitMs: number
  spaceLimitMb: number
}

export function buildProblemContentUpdateDraft(
  problem: ProblemDetail,
  editor: ProblemEditorContentState,
): UpdateProblemDraft {
  return {
    title: editor.title,
    statement: editor.statement,
    timeLimitMs: editor.timeLimitMs,
    spaceLimitMb: editor.spaceLimitMb,
    baseAccess: problem.accessPolicy.baseAccess,
    grantedUsersInput: grantedUsersInputFromAccessPolicy(problem.accessPolicy),
    grantedGroupsInput: grantedGroupsInputFromAccessPolicy(problem.accessPolicy),
    managerUsersInput: grantedManagerUsersInputFromAccessPolicy(problem.accessPolicy),
    managerGroupsInput: grantedManagerGroupsInputFromAccessPolicy(problem.accessPolicy),
    othersSubmissionAccess: problem.othersSubmissionAccess,
  }
}

export function buildProblemAccessUpdateDraft(
  problem: ProblemDetail,
  editor: ProblemEditorAccessState,
): UpdateProblemDraft {
  return {
    title: problemTitleValue(problem.title),
    statement: problemStatementTextValue(problem.statement),
    timeLimitMs: problem.timeLimitMs,
    spaceLimitMb: problem.spaceLimitMb,
    baseAccess: editor.baseAccess,
    grantedUsersInput: editor.grantedUsersInput,
    grantedGroupsInput: editor.grantedGroupsInput,
    managerUsersInput: editor.managerUsersInput,
    managerGroupsInput: editor.managerGroupsInput,
    othersSubmissionAccess: editor.othersSubmissionAccess,
  }
}

export function buildProblemDetailAccessPolicy(editor: ProblemEditorAccessState) {
  const accessPolicyResult = buildResourceAccessPolicy(
    editor.baseAccess,
    editor.grantedUsersInput,
    editor.grantedGroupsInput,
    editor.managerUsersInput,
    editor.managerGroupsInput,
  )

  return accessPolicyResult.ok
    ? accessPolicyResult.value
    : { baseAccess: editor.baseAccess, viewerGrants: [], managerGrants: [] }
}
