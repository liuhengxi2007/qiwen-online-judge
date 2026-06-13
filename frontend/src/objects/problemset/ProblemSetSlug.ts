/** 题集公开 slug 品牌类型；用于 URL 和公开 API 路径。 */
export type ProblemSetSlug = string & { readonly __brand: 'ProblemSetSlug' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

/** 创建题集 slug 品牌值；调用前必须完成格式和长度校验。 */
function createProblemSetSlug(value: string): ProblemSetSlug {
  /** 注意：这里的 as 只在 parseProblemSetSlug 校验通过后施加品牌类型。 */
  return value as ProblemSetSlug
}

/** 将题集 slug 还原为字符串；slug 语法已限制为 URL 安全字符，调用方可按路径或 query 场景继续编码。 */
export function problemSetSlugValue(slug: ProblemSetSlug): string {
  return slug
}

/** 解析题集 slug；拒绝空值、过短/过长值和非法字符。 */
export function parseProblemSetSlug(rawSlug: string): ParseResult<ProblemSetSlug> {
  const normalized = rawSlug.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem set slug is required.' }
  }
  if (normalized.length < 3 || normalized.length > 64) {
    return { ok: false, error: 'Problem set slug must be between 3 and 64 characters.' }
  }
  if (!slugPattern.test(normalized)) {
    return { ok: false, error: 'Problem set slug may contain only lowercase letters, numbers, and hyphens.' }
  }
  return { ok: true, value: createProblemSetSlug(normalized) }
}
