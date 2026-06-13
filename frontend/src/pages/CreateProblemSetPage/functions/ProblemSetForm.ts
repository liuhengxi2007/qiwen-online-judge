import { parseProblemSetDescription } from '@/objects/problemset/ProblemSetDescription'
import { parseProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { parseProblemSetTitle } from '@/objects/problemset/ProblemSetTitle'
import type { CreateProblemSetRequest } from '@/objects/problemset/request/CreateProblemSetRequest'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import { buildResourceAccessPolicy } from '@/pages/components/ResourceAccessEditorInput'

/**
 * 创建题单表单草稿，保存 slug、标题、描述、访问策略和授权输入。
 */
export type ProblemSetDraft = {
  slug: string
  title: string
  description: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
}

/**
 * 校验创建题单草稿，成功时构造后端创建请求。
 */
export function validateProblemSetDraft(
  draft: ProblemSetDraft,
): { ok: true; request: CreateProblemSetRequest } | { ok: false; message: string } {
  const slugResult = parseProblemSetSlug(draft.slug)
  if (!slugResult.ok) {
    return { ok: false, message: slugResult.error }
  }

  const titleResult = parseProblemSetTitle(draft.title)
  if (!titleResult.ok) {
    return { ok: false, message: titleResult.error }
  }

  const descriptionResult = parseProblemSetDescription(draft.description)
  if (!descriptionResult.ok) {
    return { ok: false, message: descriptionResult.error }
  }

  const accessPolicyResult = buildResourceAccessPolicy(
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
      slug: slugResult.value,
      title: titleResult.value,
      description: descriptionResult.value,
      accessPolicy: accessPolicyResult.value,
    },
  }
}
