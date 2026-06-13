/** 博客正文品牌类型；用于创建、更新和详情响应。 */
export type BlogContent = string & { readonly __brand: 'BlogContent' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建博客正文品牌值；调用前必须完成非空和长度校验。 */
function createBlogContent(value: string): BlogContent {
  /** 注意：这里的 as 只在 parseBlogContent 校验通过后施加品牌类型。 */
  return value as BlogContent
}

/** 将博客正文品牌值还原为字符串；用于 API body 和展示。 */
export function blogContentValue(content: BlogContent): string {
  return content
}

/** 解析博客正文；去除首尾空白并限制最大长度。 */
export function parseBlogContent(rawContent: string): ParseResult<BlogContent> {
  const normalized = rawContent.trim()
  if (!normalized) {
    return { ok: false, error: 'Blog content is required.' }
  }
  if (normalized.length > 200000) {
    return { ok: false, error: 'Blog content must be at most 200000 characters.' }
  }
  return { ok: true, value: createBlogContent(normalized) }
}
