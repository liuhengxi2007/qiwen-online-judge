import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }
/** 题集中题目摘要；position 表示题集内正整数排序位置。 */
export type ProblemSetProblemSummary = {
  id: ProblemId
  slug: ProblemSlug
  title: ProblemTitle
  position: number
}

/** 解析题集题目位置；只接受正安全整数，不进行重复位置检查。 */
export function parseProblemSetProblemPosition(rawPosition: number): ParseResult<number> {
  if (!Number.isSafeInteger(rawPosition) || rawPosition <= 0) {
    return { ok: false, error: 'Problem set problem position must be a positive integer.' }
  }
  return { ok: true, value: rawPosition }
}
