/** 管理账号权限的请求体；后端负责校验操作者是否具备站点管理权限。 */
export type UpdateUserPermissionsRequest = {
  siteManager: boolean
  problemManager: boolean
  contestManager: boolean
}
