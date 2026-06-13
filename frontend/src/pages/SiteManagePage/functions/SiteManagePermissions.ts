import { normalizeAuthPermissionFlags } from '@/objects/auth/AuthPermissionFlags'
import type { UpdateUserPermissionsRequest } from '@/objects/auth/request/UpdateUserPermissionsRequest'
import type { ManagedUserListItem } from '@/objects/user/response/ManagedUserListItem'

/**
 * 可在站点管理页更新的权限字段名。
 */
type PermissionKey = keyof UpdateUserPermissionsRequest
/**
 * UI 复选框状态，包含部分选中态以表达混合权限。
 */
type CheckedState = boolean | 'indeterminate'

/**
 * 从托管用户条目提取当前可展示和可编辑的权限标记。
 */
export function displayedPermissionFlags(user: ManagedUserListItem): UpdateUserPermissionsRequest {
  return normalizeAuthPermissionFlags({
    siteManager: user.siteManager,
    problemManager: user.problemManager,
    contestManager: user.contestManager,
  })
}

/**
 * 根据单个权限复选框状态生成更新请求；indeterminate 表示保持当前值不变。
 */
export function buildPermissionUpdate(
  user: ManagedUserListItem,
  permission: PermissionKey,
  checked: CheckedState,
): UpdateUserPermissionsRequest {
  const nextChecked = checked === true

  if (permission === 'siteManager') {
    if (nextChecked) {
      return {
        siteManager: true,
        problemManager: true,
        contestManager: true,
      }
    }

    return {
      siteManager: false,
      problemManager: user.problemManager,
      contestManager: user.contestManager,
    }
  }

  if (user.siteManager) {
    return {
      siteManager: true,
      problemManager: true,
      contestManager: true,
    }
  }

  return {
    siteManager: user.siteManager,
    problemManager: permission === 'problemManager' ? nextChecked : user.problemManager,
    contestManager: permission === 'contestManager' ? nextChecked : user.contestManager,
  }
}
