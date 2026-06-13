/** 题目 UUID 品牌类型；用于内部和判题接口，区别于公开 slug。 */
export type ProblemId = string & { readonly __brand: 'ProblemId' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

/** 创建题目 ID 品牌值；调用前必须完成 UUID 校验。 */
function createProblemId(value: string): ProblemId {
  /** 注意：这里的 as 只在 parseProblemId 校验通过后施加品牌类型。 */
  return value as ProblemId
}

/** 将题目 ID 品牌值还原为字符串；用于内部 API body。 */
export function problemIdValue(problemId: ProblemId): string {
  return problemId
}

/** 解析题目 ID；接受 UUID 字符串并返回结构化错误。 */
export function parseProblemId(rawId: string): ParseResult<ProblemId> {
  const normalized = rawId.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem id is required.' }
  }
  if (!uuidPattern.test(normalized)) {
    return { ok: false, error: 'Problem id must be a valid UUID.' }
  }
  return { ok: true, value: createProblemId(normalized) }
}
