/** 题目标题品牌类型；用于列表、详情和关联对象展示。 */
export type ProblemTitle = string & { readonly __brand: 'ProblemTitle' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建题目标题品牌值；调用前必须完成非空和长度校验。 */
function createProblemTitle(value: string): ProblemTitle {
  /** 注意：这里的 as 只在 parseProblemTitle 校验通过后施加品牌类型。 */
  return value as ProblemTitle
}

/** 将题目标题品牌值还原为字符串；无副作用。 */
export function problemTitleValue(title: ProblemTitle): string {
  return title
}

/** 解析题目标题；去除首尾空白并返回结构化校验结果。 */
export function parseProblemTitle(rawTitle: string): ParseResult<ProblemTitle> {
  const normalized = rawTitle.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem title is required.' }
  }
  if (normalized.length > 120) {
    return { ok: false, error: 'Problem title must be at most 120 characters.' }
  }
  return { ok: true, value: createProblemTitle(normalized) }
}
