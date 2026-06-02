export type UserAvatarUrl = string & { readonly __brand: 'UserAvatarUrl' }

export function userAvatarUrlValue(avatarUrl: UserAvatarUrl): string {
  return avatarUrl
}
