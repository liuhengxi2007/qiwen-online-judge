/** 提交列表题目搜索词品牌类型；表示已确认非空的题目过滤输入。 */
export type SubmissionProblemQuery = string & { readonly __brand: 'SubmissionProblemQuery' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建提交题目搜索词品牌值；调用前必须确认非空。 */
function createSubmissionProblemQuery(value: string): SubmissionProblemQuery {
  /** 注意：这里的 as 只在 parseSubmissionProblemQuery 校验通过后施加品牌类型。 */
  return value as SubmissionProblemQuery
}

/** 将提交题目搜索词还原为查询字符串；无副作用。 */
export function submissionProblemQueryValue(query: SubmissionProblemQuery): string {
  return query
}

/** 解析提交题目搜索输入；去除首尾空白并拒绝空查询。 */
export function parseSubmissionProblemQuery(rawQuery: string): ParseResult<SubmissionProblemQuery> {
  const normalized = rawQuery.trim()
  if (!normalized) {
    return { ok: false, error: 'Submission problem query is required.' }
  }
  return { ok: true, value: createSubmissionProblemQuery(normalized) }
}
