import type { DisplayName } from '@/objects/user/DisplayName'

/** 管理员更新用户资料的请求体；当前只允许修改显示名。 */
export type UpdateManagedUserProfileRequest = {
  displayName: DisplayName
}
