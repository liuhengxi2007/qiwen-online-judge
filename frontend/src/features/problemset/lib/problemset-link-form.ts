import { parseProblemSlug } from '@/features/problem/lib/problem-parsers'
import type { AddProblemToProblemSetRequest } from '@/features/problemset/model/request/AddProblemToProblemSetRequest'

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
