/** 账号站点权限位；siteManager 隐含题目和比赛管理权限。 */
export type AuthPermissionFlags = {
  siteManager: boolean
  problemManager: boolean
  contestManager: boolean
}

/** 归一化账号权限位；补齐 siteManager 对下级管理权限的隐含授权。 */
export function normalizeAuthPermissionFlags<T extends AuthPermissionFlags>(flags: T): T {
  return {
    ...flags,
    problemManager: flags.siteManager || flags.problemManager,
    contestManager: flags.siteManager || flags.contestManager,
  }
}
