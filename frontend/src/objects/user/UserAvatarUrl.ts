/** 用户头像 URL 品牌类型；由后端存储/返回，前端不自行校验远端资源存在性。 */
export type UserAvatarUrl = string & { readonly __brand: 'UserAvatarUrl' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建头像 URL 品牌值；调用前必须确认是可用于 img src 的 http(s) 或站内绝对路径。 */
function createUserAvatarUrl(value: string): UserAvatarUrl {
  /** 注意：这里的 as 只在 parseUserAvatarUrl 校验通过后施加品牌类型。 */
  return value as UserAvatarUrl
}

/** 解析头像 URL；拒绝 localStorage 篡改出的 javascript/data 等危险协议。 */
export function parseUserAvatarUrl(rawAvatarUrl: string): ParseResult<UserAvatarUrl> {
  const normalized = rawAvatarUrl.trim()
  if (!normalized) {
    return { ok: false, error: 'Avatar URL is required.' }
  }

  if (normalized.startsWith('/') && !normalized.startsWith('//')) {
    return { ok: true, value: createUserAvatarUrl(normalized) }
  }

  try {
    const url = new URL(normalized)
    if (url.protocol === 'http:' || url.protocol === 'https:') {
      return { ok: true, value: createUserAvatarUrl(normalized) }
    }
  } catch {
    return { ok: false, error: 'Avatar URL must be an absolute http(s) URL or a site path.' }
  }

  return { ok: false, error: 'Avatar URL must use http or https.' }
}

/** 将头像 URL 品牌值还原为字符串；用于 img src 或 API 展示。 */
export function userAvatarUrlValue(avatarUrl: UserAvatarUrl): string {
  return avatarUrl
}
