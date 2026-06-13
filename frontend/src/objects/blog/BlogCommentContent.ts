/** 博客评论正文品牌类型；用于评论和回复创建/更新请求。 */
export type BlogCommentContent = string & { readonly __brand: 'BlogCommentContent' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建评论正文品牌值；调用前必须完成非空和长度校验。 */
function createBlogCommentContent(value: string): BlogCommentContent {
  /** 注意：这里的 as 只在 parseBlogCommentContent 校验通过后施加品牌类型。 */
  return value as BlogCommentContent
}

/** 将评论正文品牌值还原为字符串；用于 API body 和展示。 */
export function blogCommentContentValue(content: BlogCommentContent): string {
  return content
}

/** 解析评论正文；去除首尾空白并限制最大长度。 */
export function parseBlogCommentContent(rawContent: string): ParseResult<BlogCommentContent> {
  const normalized = rawContent.trim()
  if (!normalized) {
    return { ok: false, error: 'Comment content is required.' }
  }
  if (normalized.length > 20000) {
    return { ok: false, error: 'Comment content must be at most 20000 characters.' }
  }
  return { ok: true, value: createBlogCommentContent(normalized) }
}
