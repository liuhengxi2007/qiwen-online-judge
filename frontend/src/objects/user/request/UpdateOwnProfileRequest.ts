import type { DisplayName } from '@/objects/user/DisplayName'

/** 用户更新自己资料的请求体；当前只允许修改显示名。 */
export type UpdateOwnProfileRequest = {
  displayName: DisplayName
}
