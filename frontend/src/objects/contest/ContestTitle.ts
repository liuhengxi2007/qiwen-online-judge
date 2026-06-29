/** 比赛标题品牌类型；用于列表、详情和编辑表单。 */
export type ContestTitle = string & { readonly __brand: 'ContestTitle' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建比赛标题品牌值；调用前必须完成非空和长度校验。 */
function createContestTitle(value: string): ContestTitle {
  /** 注意：这里的 as 只在 parseContestTitle 校验通过后施加品牌类型。 */
  return value as ContestTitle
}

/** 将比赛标题品牌值还原为字符串；无副作用。 */
export function contestTitleValue(title: ContestTitle): string {
  return title
}

/** 解析比赛标题；去除首尾空白并返回结构化校验结果。 */
export function parseContestTitle(rawTitle: string): ParseResult<ContestTitle> {
  const normalized = rawTitle.trim()
  if (!normalized) {
    return { ok: false, error: 'Contest title is required.' }
  }
  if (normalized.length > 120) {
    return { ok: false, error: 'Contest title must be at most 120 characters.' }
  }
  return { ok: true, value: createContestTitle(normalized) }
}
