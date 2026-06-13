/** 题目题面正文品牌类型；用于 markdown/文本题面内容。 */
export type ProblemStatementText = string & { readonly __brand: 'ProblemStatementText' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建题面正文品牌值；调用前必须完成非空和长度校验。 */
function createProblemStatementText(value: string): ProblemStatementText {
  /** 注意：这里的 as 只在 parseProblemStatementText 校验通过后施加品牌类型。 */
  return value as ProblemStatementText
}

/** 将题面正文品牌值还原为字符串；用于 API body 和编辑器初始值。 */
export function problemStatementTextValue(statement: ProblemStatementText): string {
  return statement
}

/** 解析题面正文；去除首尾空白并限制最大长度。 */
export function parseProblemStatementText(rawStatement: string): ParseResult<ProblemStatementText> {
  const normalized = rawStatement.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem statement is required.' }
  }
  if (normalized.length > 20000) {
    return { ok: false, error: 'Problem statement must be at most 20000 characters.' }
  }
  return { ok: true, value: createProblemStatementText(normalized) }
}
