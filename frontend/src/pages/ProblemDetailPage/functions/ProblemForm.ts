import type { OtherUserSubmissionAccess } from '@/objects/problem/OtherUserSubmissionAccess'
import { parseProblemStatementText, problemStatementTextValue } from '@/objects/problem/ProblemStatementText'
import { parseProblemTitle, problemTitleValue } from '@/objects/problem/ProblemTitle'
import type { UpdateProblemRequest } from '@/objects/problem/request/UpdateProblemRequest'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import { parseUsername, usernameValue } from '@/objects/user/Username'
import {
  buildResourceAccessPolicy,
  grantedGroupsInputFromAccessPolicy,
  grantedManagerGroupsInputFromAccessPolicy,
  grantedManagerUsersInputFromAccessPolicy,
  grantedUsersInputFromAccessPolicy,
} from '@/pages/components/ResourceAccessEditorInput'

export type UpdateProblemDraft = {
  title: string
  statement: string
  authorUsername: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
  managerUsersInput: string
  managerGroupsInput: string
  otherUserSubmissionAccess: OtherUserSubmissionAccess
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

  const rawAuthorUsername = draft.authorUsername.trim()
  const authorUsernameResult = rawAuthorUsername ? parseUsername(rawAuthorUsername) : null
  if (authorUsernameResult && !authorUsernameResult.ok) {
    return { ok: false, message: authorUsernameResult.error }
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
      accessPolicy: accessPolicyResult.value,
      otherUserSubmissionAccess: draft.otherUserSubmissionAccess,
      authorUsername: authorUsernameResult?.value ?? null,
    },
  }
}

type ProblemEditorAccessState = {
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
  managerUsersInput: string
  managerGroupsInput: string
  otherUserSubmissionAccess: OtherUserSubmissionAccess
}

type ProblemEditorContentState = ProblemEditorAccessState & {
  title: string
  statement: string
  authorUsername: string
}

export function buildProblemContentUpdateDraft(
  problem: ProblemDetail,
  editor: ProblemEditorContentState,
): UpdateProblemDraft {
  return {
    title: editor.title,
    statement: editor.statement,
    authorUsername: editor.authorUsername,
    baseAccess: problem.accessPolicy.baseAccess,
    grantedUsersInput: grantedUsersInputFromAccessPolicy(problem.accessPolicy),
    grantedGroupsInput: grantedGroupsInputFromAccessPolicy(problem.accessPolicy),
    managerUsersInput: grantedManagerUsersInputFromAccessPolicy(problem.accessPolicy),
    managerGroupsInput: grantedManagerGroupsInputFromAccessPolicy(problem.accessPolicy),
    otherUserSubmissionAccess: problem.otherUserSubmissionAccess,
  }
}

export function buildProblemAccessUpdateDraft(
  problem: ProblemDetail,
  editor: ProblemEditorAccessState,
): UpdateProblemDraft {
  return {
    title: problemTitleValue(problem.title),
    statement: problemStatementTextValue(problem.statement),
    authorUsername: problem.author ? usernameValue(problem.author.username) : '',
    baseAccess: editor.baseAccess,
    grantedUsersInput: editor.grantedUsersInput,
    grantedGroupsInput: editor.grantedGroupsInput,
    managerUsersInput: editor.managerUsersInput,
    managerGroupsInput: editor.managerGroupsInput,
    otherUserSubmissionAccess: editor.otherUserSubmissionAccess,
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
