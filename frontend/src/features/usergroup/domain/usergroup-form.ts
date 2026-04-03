import { parseUsername, type Username } from '@/features/auth/domain/auth'
import {
  parseUserGroupDescription,
  parseUserGroupName,
  parseUserGroupRole,
  parseUserGroupSlug,
  type CreateUserGroupRequest,
  type UpdateUserGroupRequest,
  type UserGroupRole,
} from '@/features/usergroup/domain/usergroup'

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
  role: string,
): { ok: true; request: { username: Username; role: UserGroupRole } } | { ok: false; message: string } {
  const usernameResult = parseUsername(username)
  if (!usernameResult.ok) {
    return { ok: false, message: usernameResult.error }
  }

  const roleResult = parseUserGroupRole(role)
  if (!roleResult.ok) {
    return { ok: false, message: roleResult.error }
  }

  return {
    ok: true,
    request: {
      username: usernameResult.value,
      role: roleResult.value,
    },
  }
}
