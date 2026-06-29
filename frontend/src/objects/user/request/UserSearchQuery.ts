/** 用户搜索词品牌类型；表示已确认非空的用户查询输入。 */
export type UserSearchQuery = string & { readonly __brand: 'UserSearchQuery' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建用户搜索词品牌值；调用前必须确认非空。 */
function createUserSearchQuery(value: string): UserSearchQuery {
  /** 注意：这里的 as 只在 parseUserSearchQuery 校验通过后施加品牌类型。 */
  return value as UserSearchQuery
}

/** 将用户搜索词品牌值还原为查询字符串；无副作用。 */
export function userSearchQueryValue(query: UserSearchQuery): string {
  return query
}

/** 解析用户搜索输入；去除首尾空白并拒绝空查询。 */
export function parseUserSearchQuery(rawQuery: string): ParseResult<UserSearchQuery> {
  const normalized = rawQuery.trim()
  if (!normalized) {
    return { ok: false, error: 'User search query is required.' }
  }
  return { ok: true, value: createUserSearchQuery(normalized) }
}
