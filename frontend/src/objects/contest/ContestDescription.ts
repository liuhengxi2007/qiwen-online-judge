/** 比赛描述品牌类型；允许为空字符串，但限制最大长度。 */
export type ContestDescription = string & { readonly __brand: 'ContestDescription' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建比赛描述品牌值；调用前必须完成长度校验。 */
function createContestDescription(value: string): ContestDescription {
  /** 注意：这里的 as 只在 parseContestDescription 校验通过后施加品牌类型。 */
  return value as ContestDescription
}

/** 将比赛描述品牌值还原为字符串；无副作用。 */
export function contestDescriptionValue(description: ContestDescription): string {
  return description
}

/** 解析比赛描述；去除首尾空白并限制最大长度。 */
export function parseContestDescription(rawDescription: string): ParseResult<ContestDescription> {
  const normalized = rawDescription.trim()
  if (normalized.length > 4000) {
    return { ok: false, error: 'Contest description must be at most 4000 characters.' }
  }
  return { ok: true, value: createContestDescription(normalized) }
}
