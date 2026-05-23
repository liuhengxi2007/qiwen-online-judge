import { parseUsername } from '@/features/user/lib/user-parsers'
import type { Username } from '@/features/user/model/Username'
import { parseUserGroupDescription, parseUserGroupName, parseUserGroupSlug } from '@/features/usergroup/lib/usergroup-parsers'
import type { AddUserGroupMemberRole } from '@/features/usergroup/model/AddUserGroupMemberRole'
import type { CreateUserGroupRequest } from '@/features/usergroup/http/request/CreateUserGroupRequest'
import type { UpdateUserGroupRequest } from '@/features/usergroup/http/request/UpdateUserGroupRequest'

export type UserGroupDraft = {
  slug: string
  name: string
  description: string
}

export function validateUserGroupDraft(
  draft: UserGroupDraft,
): { ok: true; request: CreateUserGroupRequest } | { ok: false; message: string } {
  const slugResult = parseUserGroupSlug(draft.slug)
  if (!slugResult.ok) {
    return { ok: false, message: slugResult.error }
  }

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
      slug: slugResult.value,
      name: nameResult.value,
      description: descriptionResult.value,
    },
  }
}

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
