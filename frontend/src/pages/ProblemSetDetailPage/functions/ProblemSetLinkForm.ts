import { parseProblemSlug } from '@/objects/problem/ProblemSlug'
import type { AddProblemToProblemSetRequest } from '@/objects/problemset/request/AddProblemToProblemSetRequest'

/**
 * 题单关联题目草稿，保存待关联题目 slug 输入。
 */
type ProblemSetLinkDraft = {
  problemSlug: string
}

/**
 * 题单关联题目校验结果，成功时返回解析后的题目 slug。
 */
type ProblemSetLinkValidation =
  | { ok: true; request: AddProblemToProblemSetRequest }
  | { ok: false; message: string }

/**
 * 校验题单关联题目草稿，确保 slug 符合题目 slug 规则。
 */
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
