/** 提交列表用户搜索词品牌类型；表示已确认非空的用户过滤输入。 */
export type SubmissionUserQuery = string & { readonly __brand: 'SubmissionUserQuery' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建提交用户搜索词品牌值；调用前必须确认非空。 */
function createSubmissionUserQuery(value: string): SubmissionUserQuery {
  /** 注意：这里的 as 只在 parseSubmissionUserQuery 校验通过后施加品牌类型。 */
  return value as SubmissionUserQuery
}

/** 将提交用户搜索词还原为查询字符串；无副作用。 */
export function submissionUserQueryValue(query: SubmissionUserQuery): string {
  return query
}

/** 解析提交用户搜索输入；去除首尾空白并拒绝空查询。 */
export function parseSubmissionUserQuery(rawQuery: string): ParseResult<SubmissionUserQuery> {
  const normalized = rawQuery.trim()
  if (!normalized) {
    return { ok: false, error: 'Submission username query is required.' }
  }
  return { ok: true, value: createSubmissionUserQuery(normalized) }
}
