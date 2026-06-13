import type { UserPreferences } from '@/objects/user/UserPreferences'

/** 用户更新自己偏好的请求体；目标用户由当前会话和 API path 共同确定。 */
export type UpdateOwnPreferencesRequest = {
  preferences: UserPreferences
}
