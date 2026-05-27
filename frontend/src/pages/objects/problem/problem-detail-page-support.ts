import type { OthersSubmissionAccess } from '@/objects/problem/OthersSubmissionAccess'
import type { ProblemDetail, ProblemDetail as ProblemDetailModel } from '@/objects/problem/response/ProblemDetail'
import { problemStatementTextValue, problemTitleValue } from '@/objects/problem/problem-parsers'
import { resourceAccessSubjectParsers } from '@/objects/shared/access/access-subject-parsers'
import {
  buildResourceAccessPolicy,
  grantedGroupsInputFromAccessPolicy,
  grantedManagerGroupsInputFromAccessPolicy,
  grantedManagerUsersInputFromAccessPolicy,
  grantedUsersInputFromAccessPolicy,
} from '@/objects/shared/resource-access-input'
import type { BaseAccess } from '@/objects/shared/resource-lifecycle'

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
  problem: ProblemDetailModel,
  editor: ProblemEditorContentState,
) {
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
) {
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
    resourceAccessSubjectParsers,
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
