/** 题集 UUID 品牌类型；用于内部定位题集资源。 */
export type ProblemSetId = string & { readonly __brand: 'ProblemSetId' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

/** 创建题集 ID 品牌值；调用前必须完成 UUID 校验。 */
function createProblemSetId(value: string): ProblemSetId {
  /** 注意：这里的 as 只在 parseProblemSetId 校验通过后施加品牌类型。 */
  return value as ProblemSetId
}

/** 解析题集 ID；接受 UUID 字符串并返回结构化错误。 */
export function parseProblemSetId(rawId: string): ParseResult<ProblemSetId> {
  const normalized = rawId.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem set id is required.' }
  }
  if (!uuidPattern.test(normalized)) {
    return { ok: false, error: 'Problem set id must be a valid UUID.' }
  }
  return { ok: true, value: createProblemSetId(normalized) }
}
