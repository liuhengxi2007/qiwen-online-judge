import type { OthersSubmissionAccess } from '@/objects/problem/OthersSubmissionAccess'
import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import { parseProblemSpaceLimitMb } from '@/objects/problem/ProblemSpaceLimitMb'
import { parseProblemStatementText } from '@/objects/problem/ProblemStatementText'
import { parseProblemTimeLimitMs } from '@/objects/problem/ProblemTimeLimitMs'
import { parseProblemTitle } from '@/objects/problem/ProblemTitle'
import type { CreateProblemRequest } from '@/objects/problem/request/CreateProblemRequest'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import { buildResourceAccessPolicy } from '@/pages/components/ResourceAccessEditorInput'

export type ProblemDraft = {
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
