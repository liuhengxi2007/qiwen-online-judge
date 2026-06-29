/** 博客评论 ID 品牌类型；表示后端分配的正整数评论标识。 */
export type BlogCommentId = number & { readonly __brand: 'BlogCommentId' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建博客评论 ID 品牌值；调用前必须确认是正安全整数。 */
function createBlogCommentId(value: number): BlogCommentId {
  /** 注意：这里的 as 只在 parseBlogCommentId 校验通过后施加品牌类型。 */
  return value as BlogCommentId
}

/** 将博客评论 ID 品牌值还原为数字；用于 API path。 */
export function blogCommentIdValue(id: BlogCommentId): number {
  return id
}

/** 解析博客评论 ID；拒绝非整数和非正数。 */
export function parseBlogCommentId(rawId: number): ParseResult<BlogCommentId> {
  if (!Number.isSafeInteger(rawId) || rawId <= 0) {
    return { ok: false, error: 'Blog comment id must be a positive integer.' }
  }
  return { ok: true, value: createBlogCommentId(rawId) }
}
