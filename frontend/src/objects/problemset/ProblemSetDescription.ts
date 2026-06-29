/** 题集描述品牌类型；允许为空字符串，但限制最大长度。 */
export type ProblemSetDescription = string & { readonly __brand: 'ProblemSetDescription' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建题集描述品牌值；调用前必须完成长度校验。 */
function createProblemSetDescription(value: string): ProblemSetDescription {
  /** 注意：这里的 as 只在 parseProblemSetDescription 校验通过后施加品牌类型。 */
  return value as ProblemSetDescription
}

/** 将题集描述品牌值还原为字符串；无副作用。 */
export function problemSetDescriptionValue(description: ProblemSetDescription): string {
  return description
}

/** 解析题集描述；去除首尾空白并限制最大长度。 */
export function parseProblemSetDescription(rawDescription: string): ParseResult<ProblemSetDescription> {
  const normalized = rawDescription.trim()
  if (normalized.length > 2000) {
    return { ok: false, error: 'Problem set description must be at most 2000 characters.' }
  }
  return { ok: true, value: createProblemSetDescription(normalized) }
}
