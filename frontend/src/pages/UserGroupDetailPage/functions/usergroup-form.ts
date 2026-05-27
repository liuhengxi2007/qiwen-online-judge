import { parseUsername } from '@/objects/user/Username'
import type { Username } from '@/objects/user/Username'
import type { AddUserGroupMemberRole } from '@/objects/usergroup/AddUserGroupMemberRole'
import { parseUserGroupDescription } from '@/objects/usergroup/UserGroupDescription'
import { parseUserGroupName } from '@/objects/usergroup/UserGroupName'
import type { UpdateUserGroupRequest } from '@/objects/usergroup/request/UpdateUserGroupRequest'

export type UpdateUserGroupDraft = {
  name: string
  description: string
}

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

export function validateAddUserGroupMemberDraft(
  username: string,
  role: AddUserGroupMemberRole,
): { ok: true; request: { username: Username; role: AddUserGroupMemberRole } } | { ok: false; message: string } {
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
