import { parseProblemSlug } from '@/features/problem/domain/problem'
import type { AddProblemToProblemSetRequest } from '@/features/problemset/domain/problemset'

type ProblemSetLinkDraft = {
  problemSlug: string
}

type ProblemSetLinkValidation =
  | { ok: true; request: AddProblemToProblemSetRequest }
  | { ok: false; message: string }

export function validateProblemSetLinkDraft(draft: ProblemSetLinkDraft): ProblemSetLinkValidation {
  const slugResult = parseProblemSlug(draft.problemSlug)
  if (!slugResult.ok) {
    return { ok: false, message: slugResult.error }
  }

  return {
    ok: true,
    request: {
      problemSlug: slugResult.value,
    },
  }
}
