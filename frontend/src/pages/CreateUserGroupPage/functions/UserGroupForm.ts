import { parseUserGroupDescription } from '@/objects/usergroup/UserGroupDescription'
import { parseUserGroupName } from '@/objects/usergroup/UserGroupName'
import { parseUserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import type { CreateUserGroupRequest } from '@/objects/usergroup/request/CreateUserGroupRequest'

/**
 * 创建用户组表单草稿，保存 slug、名称和描述输入。
 */
export type UserGroupDraft = {
  slug: string
  name: string
  description: string
}

/**
 * 校验创建用户组草稿，成功时构造后端创建请求。
 */
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
