/** 题目搜索词品牌类型；表示已确认非空的题目查询输入。 */
export type ProblemSearchQuery = string & { readonly __brand: 'ProblemSearchQuery' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建题目搜索词品牌值；调用前必须确认非空。 */
function createProblemSearchQuery(value: string): ProblemSearchQuery {
  /** 注意：这里的 as 只在 parseProblemSearchQuery 校验通过后施加品牌类型。 */
  return value as ProblemSearchQuery
}

/** 将题目搜索词还原为查询字符串；无副作用。 */
export function problemSearchQueryValue(query: ProblemSearchQuery): string {
  return query
}

/** 解析题目搜索输入；去除首尾空白并拒绝空查询。 */
export function parseProblemSearchQuery(rawQuery: string): ParseResult<ProblemSearchQuery> {
  const normalized = rawQuery.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem search query is required.' }
  }
  return { ok: true, value: createProblemSearchQuery(normalized) }
}
