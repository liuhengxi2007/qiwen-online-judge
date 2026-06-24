import { parseProblemSetDescription, problemSetDescriptionValue } from '@/objects/problemset/ProblemSetDescription'
import { parseProblemSetTitle, problemSetTitleValue } from '@/objects/problemset/ProblemSetTitle'
import type { UpdateProblemSetRequest } from '@/objects/problemset/request/UpdateProblemSetRequest'
import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import { parseUsername, usernameValue } from '@/objects/user/Username'
import {
  buildResourceVisibilityPolicy,
  grantedGroupsInputFromVisibilityPolicy,
  grantedUsersInputFromVisibilityPolicy,
} from '@/pages/components/ResourceAccessEditorInput'

/**
 * 题单访问控制编辑状态，保存公开级别和授权主体输入。
 */
type ProblemSetAccessEditorState = {
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
}

/**
 * 题单内容编辑状态，包含标题、描述和访问控制输入。
 */
type ProblemSetContentEditorState = ProblemSetAccessEditorState & {
  title: string
  description: string
  authorUsername: string
}

/**
 * 从题单编辑状态构造内容更新草稿。
 */
export function buildProblemSetContentUpdateDraft(
  problemSet: ProblemSetDetail,
  editor: ProblemSetContentEditorState,
): UpdateProblemSetDraft {
  return {
    title: editor.title,
    description: editor.description,
    authorUsername: editor.authorUsername,
    baseAccess: problemSet.accessPolicy.baseAccess,
    grantedUsersInput: grantedUsersInputFromVisibilityPolicy(problemSet.accessPolicy),
    grantedGroupsInput: grantedGroupsInputFromVisibilityPolicy(problemSet.accessPolicy),
  }
}

/**
 * 从题单编辑状态构造访问控制更新草稿。
 */
export function buildProblemSetAccessUpdateDraft(
  problemSet: ProblemSetDetail,
  editor: ProblemSetAccessEditorState,
): UpdateProblemSetDraft {
  return {
    title: problemSetTitleValue(problemSet.title),
    description: problemSetDescriptionValue(problemSet.description),
    authorUsername: problemSet.author ? usernameValue(problemSet.author.username) : '',
    baseAccess: editor.baseAccess,
    grantedUsersInput: editor.grantedUsersInput,
    grantedGroupsInput: editor.grantedGroupsInput,
  }
}

/**
 * 从题单访问控制编辑状态构造结构化访问策略。
 */
export function buildProblemSetDetailAccessPolicy(editor: ProblemSetAccessEditorState) {
  const accessPolicyResult = buildResourceVisibilityPolicy(
    editor.baseAccess,
    editor.grantedUsersInput,
    editor.grantedGroupsInput,
  )

  return accessPolicyResult.ok
    ? accessPolicyResult.value
    : { baseAccess: editor.baseAccess, viewerGrants: [] }
}

/**
 * 题单更新草稿，保存标题、描述和访问策略。
 */
export type UpdateProblemSetDraft = {
  title: string
  description: string
  authorUsername: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
}

/**
 * 校验题单更新草稿，成功时构造后端更新请求。
 */
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

  const rawAuthorUsername = draft.authorUsername.trim()
  const authorUsernameResult = rawAuthorUsername ? parseUsername(rawAuthorUsername) : null
  if (authorUsernameResult && !authorUsernameResult.ok) {
    return { ok: false, message: authorUsernameResult.error }
  }

  const accessPolicyResult = buildResourceVisibilityPolicy(
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
      authorUsername: authorUsernameResult?.value ?? null,
    },
  }
}
