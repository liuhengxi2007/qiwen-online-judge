import type { ResourceVisibility } from '@/shared/domain/resource-lifecycle'
import {
  parseProblemSetDescription,
  parseProblemSetSlug,
  parseProblemSetTitle,
  type CreateProblemSetRequest,
  type UpdateProblemSetRequest,
} from '@/features/problemset/domain/problemset'

export type ProblemSetDraft = {
  slug: string
  title: string
  description: string
  visibility: ResourceVisibility
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

  return {
    ok: true,
    request: {
      slug: slugResult.value,
      title: titleResult.value,
      description: descriptionResult.value,
      visibility: draft.visibility,
    },
  }
}

export type UpdateProblemSetDraft = {
  title: string
  description: string
  visibility: ResourceVisibility
}

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

  return {
    ok: true,
    request: {
      title: titleResult.value,
      description: descriptionResult.value,
      visibility: draft.visibility,
    },
  }
}
