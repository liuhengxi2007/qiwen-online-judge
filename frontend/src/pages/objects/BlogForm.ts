import { parseBlogContent } from '@/objects/blog/BlogContent'
import type { BlogContent } from '@/objects/blog/BlogContent'
import { parseBlogTitle } from '@/objects/blog/BlogTitle'
import type { BlogTitle } from '@/objects/blog/BlogTitle'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import type { ResourceVisibilityPolicy } from '@/objects/shared/access/ResourceVisibilityPolicy'
import { buildResourceVisibilityPolicy } from '@/pages/components/ResourceAccessEditorInput'

/**
 * 博客表单草稿，包含内容字段和 viewer-only 可见性输入。
 */
export type BlogFormDraft = {
  title: string
  content: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
}

/**
 * 博客创建/更新请求共享字段；创建和编辑 API 的 wire shape 保持一致。
 */
export type BlogFormRequest = {
  title: BlogTitle
  content: BlogContent
  visibilityPolicy: ResourceVisibilityPolicy
}

/**
 * 校验博客表单草稿，并构造不包含 manager grants 的可见性策略。
 */
export function validateBlogFormDraft(draft: BlogFormDraft): { ok: true; request: BlogFormRequest } | { ok: false; message: string } {
  const parsedTitle = parseBlogTitle(draft.title)
  if (!parsedTitle.ok) {
    return { ok: false, message: parsedTitle.error }
  }

  const parsedContent = parseBlogContent(draft.content)
  if (!parsedContent.ok) {
    return { ok: false, message: parsedContent.error }
  }

  const visibilityPolicyResult = buildResourceVisibilityPolicy(
    draft.baseAccess,
    draft.grantedUsersInput,
    draft.grantedGroupsInput,
  )
  if (!visibilityPolicyResult.ok) {
    return { ok: false, message: visibilityPolicyResult.message }
  }

  return {
    ok: true,
    request: {
      title: parsedTitle.value,
      content: parsedContent.value,
      visibilityPolicy: visibilityPolicyResult.value,
    },
  }
}
