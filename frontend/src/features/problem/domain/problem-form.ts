import type { CreateProblemRequest, OthersSubmissionAccess, UpdateProblemRequest } from '@/features/problem/domain/problem'
import {
  parseProblemSlug,
  parseProblemStatementText,
  parseProblemSpaceLimitMb,
  parseProblemTimeLimitMs,
  parseProblemTitle,
} from '@/features/problem/domain/problem'
import { buildResourceAccessPolicy } from '@/shared/domain/resource-access-input'
import { resourceAccessSubjectParsers } from '@/features/user/domain/resource-access-subject-parsers'
import type { BaseAccess } from '@/shared/domain/resource-lifecycle'

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
    resourceAccessSubjectParsers,    draft.baseAccess,
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
    resourceAccessSubjectParsers,    draft.baseAccess,
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
