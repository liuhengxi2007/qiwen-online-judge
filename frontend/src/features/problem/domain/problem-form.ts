import type { CreateProblemRequest } from '@/features/problem/domain/problem'
import {
  parseProblemSlug,
  parseProblemStatementText,
  parseProblemTitle,
} from '@/features/problem/domain/problem'
import type { ResourceVisibility } from '@/shared/domain/resource-lifecycle'

type ProblemDraft = {
  slug: string
  title: string
  statement: string
  visibility: ResourceVisibility
}

type ProblemDraftValidation =
  | { ok: true; request: CreateProblemRequest }
  | { ok: false; message: string }

export function validateProblemDraft(draft: ProblemDraft): ProblemDraftValidation {
  const slugResult = parseProblemSlug(draft.slug)
  if (!slugResult.ok) {
    return { ok: false, message: slugResult.error }
  }

  const titleResult = parseProblemTitle(draft.title)
  if (!titleResult.ok) {
    return { ok: false, message: titleResult.error }
  }

  const statementResult = parseProblemStatementText(draft.statement)
  if (!statementResult.ok) {
    return { ok: false, message: statementResult.error }
  }

  return {
    ok: true,
    request: {
      slug: slugResult.value,
      title: titleResult.value,
      statement: statementResult.value,
      visibility: draft.visibility,
    },
  }
}
