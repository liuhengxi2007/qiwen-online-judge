import type { UserPreferences } from '@/objects/user/UserPreferences'

/** 管理员更新用户偏好的请求体；目标用户由 API path 指定。 */
export type UpdateManagedUserPreferencesRequest = {
  preferences: UserPreferences
}
