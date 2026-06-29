/** 题目标题展示模式；控制题目列表和引用处展示标题、slug 或两者。 */
export type ProblemTitleDisplayMode = 'title' | 'slug' | 'title_with_slug'

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 返回题目标题展示模式原始枚举值；用于用户偏好持久化。 */
export function problemTitleDisplayModeValue(displayMode: ProblemTitleDisplayMode): ProblemTitleDisplayMode {
  return displayMode
}

/** 解析题目标题展示模式；拒绝未知枚举值并返回可展示错误。 */
export function parseProblemTitleDisplayMode(rawDisplayMode: string): ParseResult<ProblemTitleDisplayMode> {
  const normalized = rawDisplayMode.trim()

  switch (normalized) {
    case 'title':
    case 'slug':
    case 'title_with_slug':
      return { ok: true, value: normalized }
    default:
      return {
        ok: false,
        error: 'Problem title display mode must be one of: title, slug, title_with_slug.',
      }
  }
}
