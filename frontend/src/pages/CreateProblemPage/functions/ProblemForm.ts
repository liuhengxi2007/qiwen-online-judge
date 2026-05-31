import type { OtherUserSubmissionAccess } from '@/objects/problem/OtherUserSubmissionAccess'
import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import { parseProblemStatementText } from '@/objects/problem/ProblemStatementText'
import { parseProblemTitle } from '@/objects/problem/ProblemTitle'
import type { CreateProblemRequest } from '@/objects/problem/request/CreateProblemRequest'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import { buildResourceAccessPolicy } from '@/pages/components/ResourceAccessEditorInput'

export type ProblemDraft = {
  slug: string
  title: string
  statement: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
  managerUsersInput: string
  managerGroupsInput: string
  otherUserSubmissionAccess: OtherUserSubmissionAccess
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
      accessPolicy: accessPolicyResult.value,
      otherUserSubmissionAccess: draft.otherUserSubmissionAccess,
    },
  }
}
