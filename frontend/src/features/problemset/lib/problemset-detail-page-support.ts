import type { ProblemSetDetail } from '@/features/problemset/http/response/ProblemSetDetail'
import { problemSetDescriptionValue, problemSetTitleValue } from '@/features/problemset/lib/problemset-parsers'
import { resourceAccessSubjectParsers } from '@/shared/domain/access/access-subject-parsers'
import { buildResourceAccessPolicy, grantedGroupsInputFromAccessPolicy, grantedUsersInputFromAccessPolicy } from '@/shared/domain/resource-access-input'
import type { BaseAccess } from '@/shared/domain/resource-lifecycle'

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
) {
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
) {
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
    resourceAccessSubjectParsers,
    editor.baseAccess,
    editor.grantedUsersInput,
    editor.grantedGroupsInput,
  )

  return accessPolicyResult.ok
    ? accessPolicyResult.value
    : { baseAccess: editor.baseAccess, viewerGrants: [], managerGrants: [] }
}
