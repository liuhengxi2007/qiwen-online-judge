/** 博客标题品牌类型；用于博客创建、更新、列表和详情展示。 */
export type BlogTitle = string & { readonly __brand: 'BlogTitle' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建博客标题品牌值；调用前必须完成非空和长度校验。 */
function createBlogTitle(value: string): BlogTitle {
  /** 注意：这里的 as 只在 parseBlogTitle 校验通过后施加品牌类型。 */
  return value as BlogTitle
}

/** 将博客标题品牌值还原为字符串；无副作用。 */
export function blogTitleValue(title: BlogTitle): string {
  return title
}

/** 解析博客标题；去除首尾空白并返回结构化校验结果。 */
export function parseBlogTitle(rawTitle: string): ParseResult<BlogTitle> {
  const normalized = rawTitle.trim()
  if (!normalized) {
    return { ok: false, error: 'Blog title is required.' }
  }
  if (normalized.length > 160) {
    return { ok: false, error: 'Blog title must be at most 160 characters.' }
  }
  return { ok: true, value: createBlogTitle(normalized) }
}
