import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import type { AddProblemToProblemSetRequest } from '@/objects/problemset/request/AddProblemToProblemSetRequest'

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
