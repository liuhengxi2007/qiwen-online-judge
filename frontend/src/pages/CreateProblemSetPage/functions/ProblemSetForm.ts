import { parseProblemSetDescription } from '@/objects/problemset/ProblemSetDescription'
import { parseProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { parseProblemSetTitle } from '@/objects/problemset/ProblemSetTitle'
import type { CreateProblemSetRequest } from '@/objects/problemset/request/CreateProblemSetRequest'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import { buildResourceAccessPolicy } from '@/pages/components/ResourceAccessEditorInput'

export type ProblemSetDraft = {
  slug: string
  title: string
  description: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
}

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
