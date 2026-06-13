/** 用户头像 URL 品牌类型；由后端存储/返回，前端不自行校验远端资源存在性。 */
export type UserAvatarUrl = string & { readonly __brand: 'UserAvatarUrl' }

/** 将头像 URL 品牌值还原为字符串；用于 img src 或 API 展示。 */
export function userAvatarUrlValue(avatarUrl: UserAvatarUrl): string {
  return avatarUrl
}
