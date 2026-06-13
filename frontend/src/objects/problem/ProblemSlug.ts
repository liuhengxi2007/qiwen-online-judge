/** 题目公开 slug 品牌类型；用于 URL 和公开 API 路径。 */
export type ProblemSlug = string & { readonly __brand: 'ProblemSlug' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

/** 创建题目 slug 品牌值；调用前必须完成格式和长度校验。 */
function createProblemSlug(value: string): ProblemSlug {
  /** 注意：这里的 as 只在 parseProblemSlug 校验通过后施加品牌类型。 */
  return value as ProblemSlug
}

/** 将题目 slug 还原为路径/查询参数字符串；无副作用。 */
export function problemSlugValue(slug: ProblemSlug): string {
  return slug
}

/** 解析题目 slug；拒绝空值、过短/过长值和非法字符。 */
export function parseProblemSlug(rawSlug: string): ParseResult<ProblemSlug> {
  const normalized = rawSlug.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem slug is required.' }
  }
  if (normalized.length < 3 || normalized.length > 64) {
    return { ok: false, error: 'Problem slug must be between 3 and 64 characters.' }
  }
  if (!slugPattern.test(normalized)) {
    return { ok: false, error: 'Problem slug may contain only lowercase letters, numbers, and hyphens.' }
  }
  return { ok: true, value: createProblemSlug(normalized) }
}
