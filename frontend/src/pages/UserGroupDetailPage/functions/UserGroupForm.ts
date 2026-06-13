import { parseUsername } from '@/objects/user/Username'
import type { Username } from '@/objects/user/Username'
import type { NewUserGroupMemberRole } from '@/objects/usergroup/request/NewUserGroupMemberRole'
import { parseUserGroupDescription } from '@/objects/usergroup/UserGroupDescription'
import { parseUserGroupName } from '@/objects/usergroup/UserGroupName'
import type { UpdateUserGroupRequest } from '@/objects/usergroup/request/UpdateUserGroupRequest'

/**
 * 用户组更新表单草稿，保存待提交的名称和描述文本。
 */
export type UpdateUserGroupDraft = {
  name: string
  description: string
}

/**
 * 校验用户组更新草稿，成功时构造后端更新请求。
 */
export function validateUserGroupUpdateDraft(
  draft: UpdateUserGroupDraft,
): { ok: true; request: UpdateUserGroupRequest } | { ok: false; message: string } {
  const nameResult = parseUserGroupName(draft.name)
  if (!nameResult.ok) {
    return { ok: false, message: nameResult.error }
  }

  const descriptionResult = parseUserGroupDescription(draft.description)
  if (!descriptionResult.ok) {
    return { ok: false, message: descriptionResult.error }
  }

  return {
    ok: true,
    request: {
      name: nameResult.value,
      description: descriptionResult.value,
    },
  }
}

/**
 * 校验新增用户组成员草稿，成功时返回用户名和成员角色请求数据。
 */
export function validateAddUserGroupMemberDraft(
  username: string,
  role: NewUserGroupMemberRole,
): { ok: true; request: { username: Username; role: NewUserGroupMemberRole } } | { ok: false; message: string } {
  const usernameResult = parseUsername(username)
  if (!usernameResult.ok) {
    return { ok: false, message: usernameResult.error }
  }

  return {
    ok: true,
    request: {
      username: usernameResult.value,
      role,
    },
  }
}
